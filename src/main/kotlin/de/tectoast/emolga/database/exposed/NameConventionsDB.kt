package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.NameConventionsDB.convertOfficialToTLFull
import de.tectoast.emolga.features.PrivateCommands
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.NameConventions
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.showdown.Pokemon
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.litote.kmongo.eq
import de.tectoast.emolga.utils.json.mdb as emolgaDB

object NameConventionsDB : Table("nameconventions") {
    val GUILD = long("guild")
    val GERMAN = varchar("german", 50)
    val ENGLISH = varchar("english", 50)
    val SPECIFIED = varchar("specified", 50)
    val SPECIFIEDENGLISH = varchar("specifiedenglish", 50)

    val COMMON = bool("common").default(false)

    init {
        index("all", isUnique = false, GERMAN, ENGLISH, SPECIFIED, SPECIFIEDENGLISH, GUILD)
    }

    private val logger = KotlinLogging.logger {}

    val allNameConventions = OneTimeCache { getAll() }

    /**
     * Gets all tl names of the mons in the other language than specified
     * @param mons the list of tlnames to get (in language [lang])
     * @param lang the language of the tl
     * @param guildId the id of the guild
     * @return a list of tl names in the other language
     */
    suspend fun getAllOtherSpecified(mons: Iterable<String>, lang: Language, guildId: Long): List<String> {
        val nc = emolgaDB.nameconventions.get(guildId)
        return dbTransaction {
            val checkLang = if (lang == Language.GERMAN) SPECIFIED else SPECIFIEDENGLISH
            val resultLang = if (lang == Language.GERMAN) SPECIFIEDENGLISH else SPECIFIED
            select(resultLang).where((GUILD eq 0 or (GUILD eq guildId)) and (checkLang inList mons))
                .map { it[resultLang] }.toList() + mons.mapNotNull { mon ->
                nc.values.firstNotNullOfOrNull { it.toRegex().find(mon) }?.run {
                    select(resultLang).where { checkLang eq groupValues[1] }.firstOrNull()?.get(resultLang)
                        ?.let { repl ->
                            if (mon != value) return@let null
                            value.replace(
                                groupValues[1], repl
                            )
                        }
                }
            }
        }
    }


    private suspend fun getAll() = dbTransaction {
        select(GERMAN, ENGLISH).transform {
            emit(it[GERMAN])
            emit(it[ENGLISH])
        }.toSet()
    }

    /**
     * Checks if a given tlName is understandable for us (either because it is regular of a name convention exists
     * @param name the tlName to search for
     * @param guildId the guildId to search for
     * @param language the language of the tierlist
     */
    suspend fun checkIfExists(name: String, guildId: Long, language: Language): Boolean {
        return dbTransaction {
            selectAll().where((language.ncSpecifiedCol eq name) and (GUILD eq 0 or (GUILD eq guildId))).count() > 0
        }
    }

    /**
     * Adds a tlName to the naming conventions of the guild
     * @param tlName the tlName to add
     * @param germanName the corresponding official german name
     * @param guildId the guild id
     * @param language the language of the tierlist
     */
    suspend fun addName(tlName: String, germanName: String, guildId: Long, language: Language) {
        dbTransaction {
            if (germanName.startsWith("!")) {
                insert {
                    it[GUILD] = guildId
                    it[GERMAN] = germanName.substring(1)
                    it[ENGLISH] = germanName.substring(1)
                    it[SPECIFIED] = tlName
                    it[SPECIFIEDENGLISH] = tlName
                }
                return@dbTransaction
            }
            val row = selectAll().where(GERMAN eq germanName and (GUILD eq 0)).first()
            insert {
                it[GUILD] = guildId
                it[GERMAN] = germanName
                it[ENGLISH] = row[ENGLISH]
                when (language) {
                    Language.GERMAN -> {
                        it[SPECIFIED] = tlName
                        it[SPECIFIEDENGLISH] = row[SPECIFIEDENGLISH]
                    }

                    Language.ENGLISH -> {
                        it[SPECIFIED] = row[SPECIFIED]
                        it[SPECIFIEDENGLISH] = tlName
                    }
                }

            }
        }
    }

    suspend fun convertAllOfficialToTL(mons: Iterable<String>, guildId: Long): Map<String, String> {
        val nc = emolgaDB.nameconventions.get(guildId)
        val english = Tierlist[guildId].isEnglish
        return dbTransaction {
            selectAll().where(((GERMAN inList mons) and (GUILD eq 0 or (GUILD eq guildId))))
                .orderBy(COMMON to SortOrder.DESC, GUILD to SortOrder.ASC, SPECIFIED to SortOrder.DESC).toMap { row ->
                    row[GERMAN] to row.toTLName(english, nc)
                }
        }
    }

    private suspend fun ResultRow.toTLName(english: Boolean, nc: Map<String, String>): String {
        val official = this[GERMAN]
        val tlName = if (english) this[SPECIFIEDENGLISH] else this[SPECIFIED]
        val spec = possibleSpecs().firstOrNull { official.endsWith("-$it") }
        val finalName = if (spec != null) {
            val ncData = (nc[spec] ?: emolgaDB.defaultNameConventions()[spec]!!)
            if (tlName.matches(ncData.toRegex())) tlName
            else ncData.replace(
                "(.+)",
                tlName.substringBefore("-$spec")
            )
        } else tlName
        return finalName
    }

    /**
     * Gets all name data for the given official name
     * @param mon the official name
     * @param guildId the guild id
     * @return the [DraftName] containing the data, or null if no data could be found
     */
    suspend fun convertOfficialToTLFull(mon: String, guildId: Long): DraftName? {
        val nc = emolgaDB.nameconventions.get(guildId)
        val spec = nc.keys.firstOrNull { mon.endsWith("-$it") }
        return getDBTranslation(mon, guildId, spec, nc, english = Tierlist[guildId].isEnglish)
    }

    /**
     * Gets the tlName of a mon given the official name
     * @param mon the official name
     * @param guildId the guild id
     * @return the tlName of the mon or null if it was not a valid official name
     */
    suspend fun convertOfficialToTL(mon: String, guildId: Long): String? {
        return convertOfficialToTLFull(mon, guildId)?.tlName
    }

    /**
     * Gets the [DraftName] for a mon given the tlName (that is given on Discord)
     * @param input the (potential) tlName to look for
     * @param guildIdArg the guild id, may be overwritten by [PrivateCommands.guildForMyStuff]
     * @param english if the result should be in english
     * @return the [DraftName] or null, if no data could be found
     */
    suspend fun getDiscordTranslation(
        input: String,
        guildIdArg: Long,
        english: Boolean = false,
        officialEnglish: Boolean = english
    ): DraftName? {
        val guildId = if (guildIdArg == Constants.G.MY) PrivateCommands.guildForMyStuff ?: guildIdArg else guildIdArg
        val list = mutableListOf<Pair<String, String?>>()
        val nc = emolgaDB.nameconventions.findOne(NameConventions::guild eq guildId)?.data
        fun Map<String, String>.check() = firstNotNullOfOrNull {
            it.value.toRegex(RegexOption.IGNORE_CASE).find(input)?.let { mr -> mr to it.key }
        }

        val defaultNameConventions = emolgaDB.defaultNameConventions()
        list += input to null
        (nc?.check() ?: defaultNameConventions.check())?.also { (mr, key) ->
            list += mr.groupValues[1] + "-" + key to key
        }
        list.forEach {
            getDBTranslation(
                it.first, guildId, it.second, nc ?: defaultNameConventions, english, officialEnglish
            )?.let { d -> return d }
        }
        logger.warn("Could not find translation for $input in guild $guildId")
        return null
    }

    private val possibleSpecs = OneTimeCache { emolgaDB.defaultNameConventions().keys }

    /**
     * Gets a DraftName given the SD/Official variant of a name
     * TODO: Same as [convertOfficialToTLFull]?
     * @param input the sd/official name of a pokemon
     * @param guildId the guild id
     * @return the corresponding [DraftName] or null if no data could be found
     */
    suspend fun getSDTranslation(input: String, guildId: Long, english: Boolean = false) = getDBTranslation(
        input,
        guildId,
        possibleSpecs().firstOrNull { input.endsWith("-$it") },
        emolgaDB.nameconventions.get(guildId),
        english
    )

    /**
     * Gets all official english -> german translations given a list of official english names
     * @param list the list of official english names
     * @return a map of the official english names to the german names
     */
    suspend fun getAllTranslations(
        list: List<String>,
        predicateCol: Column<String>,
        targetCol: Column<String>
    ): Map<String, String> {
        return dbTransaction {
            select(predicateCol, targetCol).where(predicateCol inList list).toMap { it[predicateCol] to it[targetCol] }
        }
    }


    // TODO: Fuse with convertAllOfficialToTL?
    suspend fun getAllData(list: Collection<String>, checkCol: Column<String>, gid: Long): Map<String, DraftName> {
        val nc = emolgaDB.nameconventions.get(gid)
        val english = Tierlist[gid].isEnglish
        return dbTransaction {
            select(
                ENGLISH, GERMAN, SPECIFIEDENGLISH, SPECIFIED, GUILD
            ).where((checkCol inList list) and (GUILD eq 0 or (GUILD eq gid)))
                .orderBy(COMMON to SortOrder.DESC, GUILD to SortOrder.ASC, SPECIFIED to SortOrder.DESC).map {
                    val tlName = it.toTLName(english, nc)
                    DraftName(
                        if (!english) tlName else it[SPECIFIED],
                        it[GERMAN],
                        it[GUILD] != 0L,
                        if (english) tlName else it[SPECIFIEDENGLISH],
                        it[ENGLISH]
                    )
                }.toMap { it.official to it }
        }
    }

    private suspend fun getDBTranslation(
        test: String,
        guildId: Long,
        spec: String? = null,
        nc: Map<String, String>,
        english: Boolean = false,
        officialEnglish: Boolean = english,
    ): DraftName? {
        return dbTransaction {
            selectAll().where(((GERMAN eq test) or (ENGLISH eq test) or (SPECIFIED eq test) or (SPECIFIEDENGLISH eq test)) and (GUILD eq 0 or (GUILD eq guildId)))
                .orderBy(
                    GUILD to SortOrder.DESC
                ).firstOrNull()?.let {
                    return@dbTransaction DraftName(
                        it[if (english) SPECIFIEDENGLISH else SPECIFIED].let { s ->
                            if (spec != null) {
                                val ncData = (nc[spec] ?: emolgaDB.defaultNameConventions()[spec]!!)
                                if (s.matches(ncData.toRegex())) s
                                else ncData.replace(
                                    "(.+)",
                                    s.substringBefore("-$spec")
                                )
                            } else s
                        },
                        it[if (officialEnglish) ENGLISH else GERMAN],
                        it[GUILD] != 0L || spec != null,
                        if (english) it[SPECIFIED] else it[SPECIFIEDENGLISH],
                        if (officialEnglish) it[GERMAN] else it[ENGLISH]
                    )
                }
            null
        }
    }
}

/**
 * A DraftName consists of a tierlist name (tlName) and the official name (in the format that is used
 * by Pokemon Showdown).
 */
@Serializable
data class DraftName(
    val tlName: String,
    val official: String,
    @Transient val guildspecific: Boolean = false,
    @Transient val otherTl: String? = null,
    @Transient val otherOfficial: String? = null
) {
    var data: Pokemon? = null
    val displayName get() = if (official == "UNKNOWN") tlName else if (guildspecific) tlName else official
    fun tlForLanguage(language: Language) = if (language == Language.GERMAN) tlName else otherTl ?: tlName
    override fun equals(other: Any?): Boolean {
        if (other !is DraftName) return false
        return official == other.official
    }

    override fun hashCode(): Int {
        return official.hashCode()
    }
}

/**
 * Converts a Flow of type T to a Map using the provided function fn.
 * The function fn should return a Pair where the first element is the key and the second element
 * is the value to be stored in the Map.
 * The resulting Map will contain all key-value pairs collected from the Flow.
 *
 * @param T the type of elements in the Flow
 * @param A the type of keys in the Map
 * @param B the type of values in the Map
 * @param fn a function that takes an element of type T and returns a Pair<A, B>
 * @return a Map containing the key-value pairs collected from the Flow
 */
suspend fun <T, A, B> Flow<T>.toMap(fn: suspend (T) -> Pair<A, B>): Map<A, B> {
    val destination = mutableMapOf<A, B>()
    collect { value ->
        val result = fn(value)
        destination[result.first] = result.second
    }
    return destination
}

suspend fun <A, B> Flow<Pair<A, B>>.toMap() = toMap { it }
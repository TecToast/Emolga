package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.NameConventionsDB.convertOfficialToTLFull
import de.tectoast.emolga.features.PrivateCommands
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.MappedCache
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.NameConventions
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.showdown.Pokemon
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.litote.kmongo.eq
import de.tectoast.emolga.utils.json.db as emolgaDB

object NameConventionsDB : Table("nameconventions") {
    val GUILD = long("guild")
    val GERMAN = varchar("german", 50)
    val ENGLISH = varchar("english", 50)
    val SPECIFIED = varchar("specified", 50)
    val SPECIFIEDENGLISH = varchar("specifiedenglish", 50)

    val COMMON = bool("common")

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
    suspend fun getAllOtherSpecified(mons: List<String>, lang: Language, guildId: Long): List<String> {
        val nc = emolgaDB.nameconventions.get(guildId)
        return dbTransaction {
            val checkLang = if (lang == Language.GERMAN) SPECIFIED else SPECIFIEDENGLISH
            val resultLang = if (lang == Language.GERMAN) SPECIFIEDENGLISH else SPECIFIED
            select(resultLang).where((GUILD eq 0 or (GUILD eq guildId)) and (checkLang inList mons))
                .map { it[resultLang] } + mons.mapNotNull { mon ->
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

    /**
     * Gets all official names in german and english
     * @return the list of all official names
     */
    private suspend fun getAll() = dbTransaction {
        select(GERMAN, ENGLISH).flatMap { setOf(it[GERMAN], it[ENGLISH]) }.toSet()
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
            insert {
                it[GUILD] = guildId
                it[GERMAN] = germanName
                val row = selectAll().where(GERMAN eq germanName and (GUILD eq 0)).first()
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

    /**
     * Gets all name data for the given official name
     * @param mon the official name
     * @param guildId the guild id
     * @param plusOther if the other language should be included in the result
     * @return the [DraftName] containing the data, or null if no data could be found
     */
    suspend fun convertOfficialToTLFull(mon: String, guildId: Long, plusOther: Boolean = false): DraftName? {
        val nc = emolgaDB.nameconventions.get(guildId)
        val spec = nc.keys.firstOrNull { mon.endsWith("-$it") }
        return getDBTranslation(mon, guildId, spec, nc, english = Tierlist[guildId].isEnglish, plusOther = plusOther)
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
    suspend fun getDiscordTranslation(input: String, guildIdArg: Long, english: Boolean = false): DraftName? {
        val guildId = if (guildIdArg == Constants.G.MY) PrivateCommands.guildForMyStuff ?: guildIdArg else guildIdArg
        val list = mutableListOf<Pair<String, String?>>()
        val nc = emolgaDB.nameconventions.findOne(NameConventions::guild eq guildId)?.data
        fun Map<String, String>.check() = firstNotNullOfOrNull {
            it.value.toRegex().find(input)?.let { mr -> mr to it.key }
        }

        val defaultNameConventions = emolgaDB.defaultNameConventions()
        (nc?.check() ?: defaultNameConventions.check())?.also { (mr, key) ->
            list += mr.groupValues[1] + "-" + key to key
        }
        list += input to null
        list.forEach {
            getDBTranslation(
                it.first, guildId, it.second, nc ?: defaultNameConventions, english
            )?.let { d -> return d }
        }
        logger.warn("Could not find translation for $input in guild $guildId")
        return null
    }

    private val possibleSpecs = MappedCache(emolgaDB.defaultNameConventions) { it.keys }

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
    suspend fun getAllSDTranslationOnlyOfficialGerman(list: List<String>): Map<String, String> {
        return dbTransaction {
            select(ENGLISH, GERMAN).where(ENGLISH inList list).associate { it[ENGLISH] to it[GERMAN] }
        }
    }

    @Suppress("unused")
    // TODO: Check for multi lang, may be merged with [getAllSDTranslationOnlyOfficialGerman]
    suspend fun getAllSDTranslationOnlyOfficialEnglish(list: List<String>): Map<String, String> {
        return dbTransaction {
            select(ENGLISH, GERMAN).where(GERMAN inList list).associate { it[GERMAN] to it[ENGLISH] }
        }
    }

    private suspend fun getDBTranslation(
        test: String,
        guildId: Long,
        spec: String? = null,
        nc: Map<String, String>,
        english: Boolean = false,
        plusOther: Boolean = false
    ): DraftName? {
        return dbTransaction {
            selectAll().where(((GERMAN eq test) or (ENGLISH eq test) or (SPECIFIED eq test) or (SPECIFIEDENGLISH eq test)) and (GUILD eq 0 or (GUILD eq guildId)))
                .orderBy(
                    GUILD to SortOrder.DESC
                ).firstOrNull()?.let {
                    return@dbTransaction DraftName(
                        it[if (english) SPECIFIEDENGLISH else SPECIFIED].let { s ->
                            if (spec != null) {
                                // TODO: Rework (and add docs)
                                val pattern = nc[spec] ?: emolgaDB.defaultNameConventions()[spec]!!
                                val len = pattern.replace("(.+)", "").length
                                val replace = pattern.replace("(.+)", s.substringBefore("-$spec"))
                                //logger.warn("s: {}, raw: {}, len: {}, replace: {}", s, raw, len, replace)
                                val replLen = replace.length
                                val coercedLen = len.coerceAtMost(replLen)
                                if (replace.substring(0, coercedLen) == replace.substring(
                                        coercedLen, (2 * len).coerceAtMost(replLen)
                                    )
                                ) replace.substring(coercedLen) else if (replace.substring(replLen - len) == replace.substring(
                                        (replLen - 2 * len).coerceAtLeast(0), replLen - len
                                    )
                                ) replace.substring(
                                    0, replLen - len
                                ) else replace
                            } else s
                        },
                        it[if (english) ENGLISH else GERMAN],
                        it[GUILD] != 0L || spec != null,
                        if (plusOther) if (english) it[SPECIFIED] else it[SPECIFIEDENGLISH] else null,
                        if (plusOther) if (english) it[GERMAN] else it[ENGLISH] else null
                    )
                }
            null
        }
    }
}

/**
 * A DraftName consists of a tierlist name (tlName) and the official name (in the format that is used
 * by Pokemon Showdown.
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
    val displayName get() = if (guildspecific) tlName else official
    override fun equals(other: Any?): Boolean {
        if (other !is DraftName) return false
        return official == other.official
    }

    override fun hashCode(): Int {
        return official.hashCode()
    }
}

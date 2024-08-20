package de.tectoast.emolga.database.exposed

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
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.litote.kmongo.eq
import de.tectoast.emolga.utils.json.db as emolgaDB

object NameConventionsDB : Table("nameconventions") {
    val GUILD = long("guild")
    val GERMAN = varchar("german", 50)
    val ENGLISH = varchar("english", 50)
    val SPECIFIED = varchar("specified", 50)
    val SPECIFIEDENGLISH = varchar("specifiedenglish", 50)
    val HASHYPHENINNAME = bool("hashypheninname")

    val COMMON = bool("common")

    private val logger = KotlinLogging.logger {}

    val allNameConventions = OneTimeCache { getAll() }

    suspend fun getAllOtherSpecified(mons: List<String>, lang: Language, guildId: Long): List<String> {
        val nc = emolgaDB.nameconventions.get(guildId)
        return newSuspendedTransaction {
            val checkLang = if (lang == Language.GERMAN) SPECIFIED else SPECIFIEDENGLISH
            val resultLang = if (lang == Language.GERMAN) SPECIFIEDENGLISH else SPECIFIED
            selectAll().where((GUILD eq 0 or (GUILD eq guildId)) and (checkLang inList mons))
                .map { it[if (lang == Language.GERMAN) SPECIFIEDENGLISH else SPECIFIED] } + mons.mapNotNull { mon ->
                nc.values.firstNotNullOfOrNull { it.toRegex().find(mon) }?.run {
                    selectAll().where { checkLang eq groupValues[1] }.firstOrNull()?.get(resultLang)?.let { repl ->
                        if (mon != value) return@let null
                        value.replace(
                            groupValues[1], repl
                        )
                    }
                }
            }
        }
    }

    private suspend fun getAll() = newSuspendedTransaction {
        selectAll().flatMap { setOf(it[GERMAN], it[ENGLISH]) }.toSet()
    }

    suspend fun insertDefault(english: String, name: String) {
        newSuspendedTransaction {
            insert {
                it[GUILD] = 0
                it[GERMAN] = name
                it[this.ENGLISH] = english
                it[SPECIFIED] = name
            }
        }
    }

    suspend fun insertDefaultCosmetic(
        english: String, german: String, specifiedEnglish: String, specifiedGerman: String
    ) {
        newSuspendedTransaction {
            insert {
                it[GUILD] = 1
                it[this.GERMAN] = german
                it[this.ENGLISH] = english
                it[SPECIFIED] = specifiedGerman
                it[SPECIFIEDENGLISH] = specifiedEnglish
            }
        }
    }

    suspend fun checkIfExists(name: String, guildId: Long): Boolean {
        return newSuspendedTransaction {
            selectAll().where((SPECIFIED eq name) and (GUILD eq 0 or (GUILD eq guildId))).firstOrNull() != null
        }
    }

    suspend fun addName(tlName: String, germanName: String, guildId: Long) {
        newSuspendedTransaction {
            insert {
                it[GUILD] = guildId
                it[GERMAN] = germanName
                val row = selectAll().where(GERMAN eq germanName and (GUILD eq 0)).first()
                it[ENGLISH] = row[ENGLISH]
                it[SPECIFIED] = tlName
                it[SPECIFIEDENGLISH] = /*run {
                    val baseName = row[GERMAN].split("-").let { base ->
                        if (row[HASHYPHENINNAME]) base.take(2).joinToString("-") else base[0]
                    }
                    tlName.replace(baseName, row[ENGLISH].split("-").first())
                }*/ row[SPECIFIEDENGLISH]
            }
        }
    }

    suspend fun convertOfficialToTLFull(mon: String, guildId: Long, plusOther: Boolean = false): DraftName? {
        val nc = emolgaDB.nameconventions.get(guildId)
        val spec = nc.keys.firstOrNull { mon.endsWith("-$it") }
        return getDBTranslation(mon, guildId, spec, nc, english = Tierlist[guildId].isEnglish, plusOther = plusOther)
    }

    suspend fun convertOfficialToTL(mon: String, guildId: Long): String? {
        return convertOfficialToTLFull(mon, guildId)?.tlName
    }

    suspend fun getDiscordTranslation(s: String, guildIdArg: Long, english: Boolean = false): DraftName? {
        val guildId = if (guildIdArg == Constants.G.MY) PrivateCommands.guildForMyStuff ?: guildIdArg else guildIdArg
        val list = mutableListOf<Pair<String, String?>>()
        val nc = emolgaDB.nameconventions.findOne(NameConventions::guild eq guildId)?.data
        fun Map<String, String>.check() = firstNotNullOfOrNull {
            it.value.toRegex().find(s)?.let { mr -> mr to it.key }
        }

        val defaultNameConventions = emolgaDB.defaultNameConventions()
        (nc?.check() ?: defaultNameConventions.check())?.also { (mr, key) ->
            list += mr.groupValues[1] + "-" + key to key
        }
        list += s to null
        list.forEach {
            getDBTranslation(
                it.first, guildId, it.second, nc ?: defaultNameConventions, english
            )?.let { d -> return d }
        }
        logger.warn("Could not find translation for $s in guild $guildId")
        return null
    }

    private val possibleSpecs = MappedCache(emolgaDB.defaultNameConventions) { it.keys }

    suspend fun getSDTranslation(s: String, guildId: Long, english: Boolean = false) = getDBTranslation(
        s, guildId, possibleSpecs().firstOrNull { s.endsWith("-$it") }, emolgaDB.nameconventions.get(guildId), english
    )

    suspend fun getAllSDTranslationOnlyOfficialGerman(list: List<String>): Map<String, String> {
        return newSuspendedTransaction {
            selectAll().where(ENGLISH inList list).associate { it[ENGLISH] to it[GERMAN] }
        }
    }

    @Suppress("unused")
    suspend fun getAllSDTranslationOnlyOfficialEnglish(list: List<String>): Map<String, String> {
        return newSuspendedTransaction {
            selectAll().where(GERMAN inList list).associate { it[GERMAN] to it[ENGLISH] }
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
        return newSuspendedTransaction {
            selectAll().where(((GERMAN eq test) or (ENGLISH eq test) or (SPECIFIED eq test) or (SPECIFIEDENGLISH eq test)) and (GUILD eq 0 or (GUILD eq guildId)))
                .orderBy(
                    GUILD to SortOrder.DESC
                ).firstOrNull()?.let {
                    return@newSuspendedTransaction DraftName(
                        it[if (english) SPECIFIEDENGLISH else SPECIFIED].let { s ->
                            if (spec != null) {
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

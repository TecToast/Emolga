package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Language
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.NameConventions
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.showdown.Pokemon
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.eq

object NameConventionsDB : Table("nameconventions") {
    val GUILD = long("guild")
    val GERMAN = varchar("german", 50)
    val ENGLISH = varchar("english", 50)
    private val SPECIFIED = varchar("specified", 50)
    private val SPECIFIEDENGLISH = varchar("specifiedenglish", 50)
    private val HASHYPHENINNAME = bool("hashypheninname")

    @Suppress("unused") // used in DB but not here
    private val COMMON = bool("common")

    private val logger = KotlinLogging.logger {}

    suspend fun getAllOtherSpecified(mons: List<String>, lang: Language, guildId: Long): List<String> {
        val nc = db.nameconventions.get(guildId)
        return transaction {
            val checkLang = if (lang == Language.GERMAN) SPECIFIED else SPECIFIEDENGLISH
            val resultLang = if (lang == Language.GERMAN) SPECIFIEDENGLISH else SPECIFIED
            select(checkLang inList mons).map { it[if (lang == Language.GERMAN) SPECIFIEDENGLISH else SPECIFIED] } +
                    mons.mapNotNull { mon ->
                        nc.values.firstNotNullOfOrNull { it.toRegex().find(mon) }?.run {
                            select { checkLang eq groupValues[1] }.firstOrNull()
                                ?.get(resultLang)?.let { repl ->
                                    if (mon != value) return@let null
                                    value.replace(
                                        groupValues[1],
                                        repl
                                    )
                                }
                        }
                    }
        }
    }

    fun getAll() = transaction {
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
            select(((SPECIFIED eq name) or (SPECIFIEDENGLISH eq name)) and (GUILD eq 0 or (GUILD eq guildId))).firstOrNull() != null
        }
    }

    suspend fun addName(tlName: String, germanName: String, guildId: Long) {
        newSuspendedTransaction {
            insert {
                it[GUILD] = guildId
                it[GERMAN] = germanName
                val row = select(GERMAN eq germanName).first()
                it[ENGLISH] = row[ENGLISH]
                it[SPECIFIED] = tlName
                it[SPECIFIEDENGLISH] = run {
                    val baseName = row[GERMAN].split("-").let { base ->
                        if (row[HASHYPHENINNAME]) base.take(2).joinToString("-") else base[0]
                    }
                    tlName.replace(baseName, row[ENGLISH].split("-").first())
                }
            }
        }
    }

    suspend fun convertOfficialToTL(mon: String, guildId: Long): String? {
        val nc = db.nameconventions.get(guildId)
        val spec = nc.keys.firstOrNull { mon.endsWith("-$it") }
        return getDBTranslation(mon, guildId, spec, nc, english = Tierlist[guildId].isEnglish)?.tlName
    }

    suspend fun getDiscordTranslation(s: String, guildId: Long, english: Boolean = false): DraftName? {
        val list = mutableListOf<Pair<String, String?>>()
        val nc = db.nameconventions.findOne(NameConventions::guild eq guildId)?.data
        fun Map<String, String>.check() = firstNotNullOfOrNull {
            it.value.toRegex().find(s)?.let { mr -> mr to it.key }
        }
        val defaultNameConventions = db.defaultNameConventions
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

    private val possibleSpecs by lazy { db.defaultNameConventions.keys }

    suspend fun getSDTranslation(s: String, guildId: Long, english: Boolean = false) = getDBTranslation(
        s,
        guildId,
        possibleSpecs.firstOrNull { s.endsWith("-$it") },
        db.nameconventions.get(guildId),
        english
    )

    suspend fun getAllSDTranslationOnlyOfficialGerman(list: List<String>): Map<String, String> {
        return newSuspendedTransaction {
            select(ENGLISH inList list).associate { it[ENGLISH] to it[GERMAN] }
        }
    }

    private suspend fun getDBTranslation(
        test: String, guildId: Long, spec: String? = null, nc: Map<String, String>, english: Boolean = false
    ): DraftName? {
        return newSuspendedTransaction {
            select(((GERMAN eq test) or (ENGLISH eq test) or (SPECIFIED eq test) or (SPECIFIEDENGLISH eq test)) and (GUILD eq 0 or (GUILD eq guildId))).orderBy(
                GUILD to SortOrder.DESC
            ).firstOrNull()?.let {
                return@newSuspendedTransaction DraftName(
                    //if ("-" in it[specified] && "-" !in it[german]) it[german] else it[specified],
                    it[if (english) SPECIFIEDENGLISH else SPECIFIED].let { s ->
                        if (spec != null) {
                            val pattern = nc[spec] ?: de.tectoast.emolga.utils.json.db.defaultNameConventions[spec]!!
                            val len = pattern.replace("(\\S+)", "").length
                            val replace = pattern.replace("(\\S+)", s.substringBefore("-$spec"))
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
                    }, it[if (english) ENGLISH else GERMAN], it[GUILD] != 0L || spec != null
                )
            }
            null
        }
    }
}

data class DraftName(val tlName: String, val official: String, val guildspecific: Boolean = false) {
    var data: Pokemon? = null
    val displayName get() = if (guildspecific) tlName else official
}

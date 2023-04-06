package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Language
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.Emolga
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object NameConventionsDB : Table("nameconventions") {
    val GUILD = long("guild")
    val GERMAN = varchar("german", 50)
    private val ENGLISH = varchar("english", 50)
    private val SPECIFIED = varchar("specified", 50)
    private val SPECIFIEDENGLISH = varchar("specifiedenglish", 50)
    private val HASHYPHENINNAME = bool("hashypheninname")

    fun getAllOtherSpecified(mons: List<String>, lang: Language): List<String> {
        return transaction {
            select((if (lang == Language.GERMAN) SPECIFIED else SPECIFIEDENGLISH) inList mons).map { it[if (lang == Language.GERMAN) SPECIFIEDENGLISH else SPECIFIED] }
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

    fun convertOfficialToTL(mon: String, guildId: Long): String? {
        val nc = Emolga.get.nameconventions[guildId] ?: Emolga.get.defaultNameConventions
        val spec = nc.keys.firstOrNull { mon.endsWith("-$it") }
        return getDBTranslation(mon, guildId, spec, nc, english = Tierlist[guildId].isEnglish)?.tlName
    }

    fun getDiscordTranslation(s: String, guildId: Long, english: Boolean = false): DraftName? {
        val list = mutableListOf<Pair<String, String?>>()
        val nc = Emolga.get.nameconventions[guildId]
        fun Map<String, Regex>.check() = firstNotNullOfOrNull {
            it.value.find(s)?.let { mr -> mr to it.key }
        }
        (nc?.check() ?: Emolga.get.nameconventions[0]!!.check())?.also { (mr, key) ->
            list += mr.groupValues[1] + "-" + key to key
        }
        list += s to null
        list.forEach {
            getDBTranslation(
                it.first, guildId, it.second, nc ?: Emolga.get.defaultNameConventions, english
            )?.let { d -> return d }
        }
        return null
    }

    private val possibleSpecs by lazy { Emolga.get.nameconventions[0]!!.keys }

    fun getSDTranslation(s: String, guildId: Long, english: Boolean = false) = getDBTranslation(
        s,
        guildId,
        possibleSpecs.firstOrNull { s.endsWith("-$it") },
        Emolga.get.nameconventions[guildId] ?: Emolga.get.defaultNameConventions,
        english
    )

    private fun getDBTranslation(
        test: String, guildId: Long, spec: String? = null, nc: Map<String, Regex>, english: Boolean = false
    ): DraftName? {
        return transaction {
            select(((GERMAN eq test) or (ENGLISH eq test) or (SPECIFIED eq test) or (SPECIFIEDENGLISH eq test)) and (GUILD eq 0 or (GUILD eq guildId))).orderBy(
                GUILD to SortOrder.DESC
            ).firstOrNull()?.let {
                return@transaction DraftName(
                    //if ("-" in it[specified] && "-" !in it[german]) it[german] else it[specified],
                    it[if (english) SPECIFIEDENGLISH else SPECIFIED].let { s ->
                        if (spec != null) {
                            val pattern = nc[spec]!!.pattern
                            val raw = pattern.replace("(\\S+)", "")
                            val len = raw.length
                            val replace = pattern.replace("(\\S+)", s.substringBefore("-$spec"))
                            if (replace.substring(0, len) == replace.substring(
                                    len, 2 * len
                                )
                            ) replace.substring(len) else if (replace.substring(replace.length - len) == replace.substring(
                                    replace.length - 2 * len, replace.length - len
                                )
                            ) replace.substring(
                                0, replace.length - len
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
    val displayName get() = if (guildspecific) tlName else official
}

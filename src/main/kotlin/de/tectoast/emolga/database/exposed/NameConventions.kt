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

object NameConventions : Table("nameconventions") {
    val guild = long("guild")
    val german = varchar("german", 50)
    private val english = varchar("english", 50)
    private val specified = varchar("specified", 50)
    private val specifiedenglish = varchar("specifiedenglish", 50)
    private val hashypheninname = bool("hashypheninname")

    fun getAllOtherSpecified(mons: List<String>, lang: Language): List<String> {
        return transaction {
            select((if (lang == Language.GERMAN) specified else specifiedenglish) inList mons).map { it[if (lang == Language.GERMAN) specifiedenglish else specified] }
        }
    }

    fun getAll() = transaction {
        selectAll().flatMap { setOf(it[german], it[english]) }.toSet()
    }

    suspend fun insertDefault(english: String, name: String) {
        newSuspendedTransaction {
            insert {
                it[guild] = 0
                it[german] = name
                it[this.english] = english
                it[specified] = name
            }
        }
    }

    suspend fun insertDefaultCosmetic(
        english: String, german: String, specifiedEnglish: String, specifiedGerman: String
    ) {
        newSuspendedTransaction {
            insert {
                it[guild] = 1
                it[this.german] = german
                it[this.english] = english
                it[specified] = specifiedGerman
                it[specifiedenglish] = specifiedEnglish
            }
        }
    }

    suspend fun checkIfExists(name: String, guildId: Long): Boolean {
        return newSuspendedTransaction {
            select(((specified eq name) or (specifiedenglish eq name)) and (guild eq 0 or (guild eq guildId))).firstOrNull() != null
        }
    }

    suspend fun addName(tlName: String, germanName: String, guildId: Long) {
        newSuspendedTransaction {
            insert {
                it[guild] = guildId
                it[german] = germanName
                val row = select(german eq germanName).first()
                it[english] = row[english]
                it[specified] = tlName
                it[specifiedenglish] = run {
                    val baseName = row[german].split("-").let { base ->
                        if (row[hashypheninname]) base.take(2).joinToString("-") else base[0]
                    }
                    tlName.replace(baseName, row[english].split("-").first())
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
            select(((german eq test) or (NameConventions.english eq test) or (specified eq test) or (specifiedenglish eq test)) and (guild eq 0 or (guild eq guildId))).orderBy(
                guild to SortOrder.DESC
            ).firstOrNull()?.let {
                return@transaction DraftName(
                    //if ("-" in it[specified] && "-" !in it[german]) it[german] else it[specified],
                    it[if (english) specifiedenglish else specified].let { s ->
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
                    }, it[if (english) NameConventions.english else german], it[guild] != 0L || spec != null
                )
            }
            null
        }
    }
}

data class DraftName(val tlName: String, val official: String, val guildspecific: Boolean = false) {
    val displayName get() = if (guildspecific) tlName else official
}

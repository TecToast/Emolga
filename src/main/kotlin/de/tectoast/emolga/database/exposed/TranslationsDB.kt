package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.upsert
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.Translation
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object TranslationsDB : Table("translations") {
    val ENGLISHID = varchar("englishid", 30)
    val GERMANID = varchar("germanid", 30)
    val ENGLISHNAME = varchar("englishname", 30)
    val GERMANNAME = varchar("germanname", 30)
    val TYPE = varchar("type", 7)
    val MODIFICATION = varchar("modification", 7).default("default")
    val ISNICK = bool("isnick").default(false)
    val FORME = varchar("forme", 10).nullable()
    val CAP = bool("cap").default(false)

    override val primaryKey = PrimaryKey(ENGLISHID, TYPE)

    fun getTranslation(
        id: String,
        checkOnlyEnglish: Boolean,
        language: Language,
        withCap: Boolean = false,
        modification: String = "default"
    ) =
        transaction {
            select { (ENGLISHID eq id).let { if (checkOnlyEnglish) it else it or (GERMANID eq id) } and (CAP eq withCap) and (MODIFICATION eq modification) }.firstOrNull()
                ?.let {
                    Translation(
                        it[language.translationCol],
                        Translation.Type.fromId(it[TYPE]),
                        language,
                        otherLang = it[language.otherCol],
                        forme = it[FORME]
                    )
                }
        }

    fun getEnglishIdsAndGermanNames(col: Collection<String>) = transaction {
        select { ENGLISHID inList col }.map { it[ENGLISHID] to it[GERMANNAME] }
    }

    fun addNick(nick: String, t: Translation) = transaction {
        upsert(
            nick,
            mapOf(
                GERMANID to nick,
                ENGLISHNAME to t.otherLang,
                GERMANNAME to t.translation,
                TYPE to t.type,
                ISNICK to true
            ),
            ENGLISHID
        )
    }

    fun removeNick(nick: String) = transaction {
        deleteWhere { (ENGLISHID eq nick) and (ISNICK eq true) } != 0
    }

}

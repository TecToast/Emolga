package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.Translation
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object TranslationsDB : Table("translations") {
    val ENGLISHID = varchar("englishid", 30)
    val GERMANID = varchar("germanid", 30)
    val ENGLISHNAME = varchar("englishname", 30)
    val GERMANNAME = varchar("germanname", 30)
    val TYPE = varchar("type", 7)
    val MODIFICATION = varchar("modification", 7).default("default")
    val FORME = varchar("forme", 10).nullable()
    val CAP = bool("cap").default(false)

    override val primaryKey = PrimaryKey(ENGLISHID, TYPE)

    suspend fun getTranslation(
        id: String,
        checkOnlyEnglish: Boolean,
        language: Language,
        withCap: Boolean = false,
        modification: String = "default"
    ) =
        newSuspendedTransaction {
            selectAll().where { (ENGLISHID eq id).let { if (checkOnlyEnglish) it else it or (GERMANID eq id) } and (CAP eq withCap) and (MODIFICATION eq modification) }
                .firstOrNull()
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


}

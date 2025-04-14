package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.Language
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.or


object TypesDB : Table("types") {
    val ENGLISHID = varchar("englishid", 30)
    val GERMANID = varchar("germanid", 30)
    val ENGLISHNAME = varchar("englishname", 30)
    val GERMANNAME = varchar("germanname", 30)

    override val primaryKey = PrimaryKey(ENGLISHID)

    suspend fun getType(
        input: String,
        language: Language,
    ) = dbTransaction {
        val id = input.lowercase()
        select(language.translationCol).where { (ENGLISHID eq id) or (GERMANID eq id) }
            .firstOrNull()?.get(language.translationCol)
    }

    suspend fun getTypeInBothLanguages(
        input: String,
    ) = dbTransaction {
        val id = input.lowercase()
        select(ENGLISHNAME, GERMANNAME).where { (ENGLISHID eq id) or (GERMANID eq id) }
            .firstOrNull()?.let { Translation(it[GERMANNAME], it[ENGLISHNAME]) }
    }

    suspend fun getOptions(input: String) = dbTransaction {
        val search = "%${input.lowercase()}%"
        select(ENGLISHNAME, GERMANNAME).where { (ENGLISHID like search) or (GERMANID like search) }
            .flatMap { res -> listOf(ENGLISHNAME, GERMANNAME).map { res[it] } }
    }

}

data class Translation(val german: String, val english: String)

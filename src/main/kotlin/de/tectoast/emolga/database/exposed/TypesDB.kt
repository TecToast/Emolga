package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.union


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
        selectAll().where { (ENGLISHID like search) }.union(selectAll().where { (GERMANID like search) })
            .map { it[ENGLISHNAME] }.distinct()
    }

}

suspend fun <T> Flow<T>.distinct() = toSet().toList()

data class Translation(val german: String, val english: String)

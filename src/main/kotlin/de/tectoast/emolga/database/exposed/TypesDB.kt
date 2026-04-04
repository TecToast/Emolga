package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.Language
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.union
import org.koin.core.annotation.Single

object TypesTable : Table("types") {
    val englishid = varchar("englishid", 30)
    val germanid = varchar("germanid", 30)
    val englishname = varchar("englishname", 30)
    val germanname = varchar("germanname", 30)

    override val primaryKey = PrimaryKey(englishid)
}

@Single
class TypesRepository(val db: R2dbcDatabase) {
    suspend fun getType(
        input: String,
        language: Language,
    ) = suspendTransaction(db) {
        val id = input.lowercase()
        TypesTable.select(language.translationCol).where { (TypesTable.englishid eq id) or (TypesTable.germanid eq id) }
            .firstOrNull()?.get(language.translationCol)
    }

    suspend fun getTypeInBothLanguages(
        input: String,
    ) = suspendTransaction(db) {
        val id = input.lowercase()
        TypesTable.select(TypesTable.englishname, TypesTable.germanname)
            .where { (TypesTable.englishid eq id) or (TypesTable.germanid eq id) }
            .firstOrNull()?.let { Translation(it[TypesTable.germanname], it[TypesTable.englishname]) }
    }

    suspend fun getOptions(input: String) = suspendTransaction(db) {
        val search = "%${input.lowercase()}%"
        TypesTable.select(TypesTable.englishname).where { (TypesTable.englishid like search) }
            .union(TypesTable.select(TypesTable.englishname).where { (TypesTable.germanid like search) })
            .map { it[TypesTable.englishname] }.toSet().toList()
    }
}

data class Translation(val german: String, val english: String)

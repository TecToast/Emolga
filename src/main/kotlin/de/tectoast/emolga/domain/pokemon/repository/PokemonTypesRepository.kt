package de.tectoast.emolga.domain.pokemon.repository

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


@Single
class PokemonTypesRepository(private val db: R2dbcDatabase) {
    suspend fun getType(
        input: String,
        language: Language,
    ) = suspendTransaction(db) {
        val id = input.lowercase()
        val translationCol = when (language) {
            Language.GERMAN -> PokemonTypesTable.germanname
            Language.ENGLISH -> PokemonTypesTable.englishname
        }
        PokemonTypesTable.select(translationCol)
            .where { (PokemonTypesTable.englishid eq id) or (PokemonTypesTable.germanid eq id) }
            .firstOrNull()?.get(translationCol)
    }

    suspend fun getOptions(input: String) = suspendTransaction(db) {
        val search = "%${input.lowercase()}%"
        PokemonTypesTable.select(PokemonTypesTable.englishname).where { (PokemonTypesTable.englishid like search) }
            .union(
                PokemonTypesTable.select(PokemonTypesTable.englishname)
                    .where { (PokemonTypesTable.germanid like search) })
            .map { it[PokemonTypesTable.englishname] }.toSet().toList()
    }
}

object PokemonTypesTable : Table("types") {
    val englishid = text("englishid")
    val germanid = text("germanid")
    val englishname = text("englishname")
    val germanname = text("germanname")

    override val primaryKey = PrimaryKey(englishid)
}

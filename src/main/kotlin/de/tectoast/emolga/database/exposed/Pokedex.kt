package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.json.showdown.Pokemon
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

object PokedexTable : Table("pokedex") {
    val id = varchar("id", 50)
    val data = jsonb<Pokemon>("data")

    override val primaryKey = PrimaryKey(id)
}

@Single
class PokedexRepository(val db: R2dbcDatabase) {

    suspend fun get(id: String): Pokemon? = suspendTransaction(db) {
        PokedexTable.select(PokedexTable.data).where { PokedexTable.id eq id }
            .map { it[PokedexTable.data] }.firstOrNull()
    }

    suspend fun getAll(ids: Iterable<String>): Map<String, Pokemon> = suspendTransaction(db) {
        PokedexTable.select(PokedexTable.id, PokedexTable.data).where { PokedexTable.id inList ids }
            .toMap { it[PokedexTable.id] to it[PokedexTable.data] }
    }

}
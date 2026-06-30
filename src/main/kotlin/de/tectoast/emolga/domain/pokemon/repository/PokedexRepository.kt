package de.tectoast.emolga.domain.pokemon.repository

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.pokemon.model.Pokemon
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.database.showdownIDColumn
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.toShowdownID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.associateTo
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class PokedexRepository(private val db: R2dbcDatabase) : StartupTask {
    private val ready = CompletableDeferred<Unit>()
    private val pokedex = mutableMapOf<ShowdownID, Pokemon>()

    override suspend fun onStartup() {
        suspendTransaction(db) {
            PokedexTable.selectAll().map { it[PokedexTable.id] to it[PokedexTable.data] }.associateTo(pokedex) { it }
        }
        ready.complete(Unit)
    }

    private suspend inline fun <T> withReady(block: suspend () -> T): T {
        ready.await()
        return block()
    }

    suspend fun getPokedexNumber(showdownId: ShowdownID): Int? = withReady {
        pokedex[showdownId]?.num
    }

    suspend fun getPokedexNumbers(showdownIds: Iterable<ShowdownID>): Map<ShowdownID, Int> = withReady {
        val destination = mutableMapOf<ShowdownID, Int>()
        for (id in showdownIds) {
            pokedex[id]?.num?.let { destination[id] = it }
        }
        destination
    }

    suspend fun get(id: ShowdownID): Pokemon? = withReady {
        pokedex[id]
    }

    suspend fun getAll(ids: Iterable<ShowdownID>): Map<ShowdownID, Pokemon> = withReady {
        val destination = mutableMapOf<ShowdownID, Pokemon>()
        for (id in ids) {
            pokedex[id]?.let { destination[id] = it }
        }
        destination
    }

    suspend fun getAllPossibleForms(ids: Iterable<ShowdownID>): Map<ShowdownID, List<ShowdownID>> = withReady {
        val destination = mutableMapOf<ShowdownID, List<ShowdownID>>()
        for (id in ids) {
            pokedex[id]?.let { pokemon ->
                val formes = pokemon.otherFormes
                val baseSpecies = pokemon.baseSpecies
                destination[id] = buildList {
                    add(id)
                    if (formes != null) addAll(formes.map(String::toShowdownID))
                    if (baseSpecies != null) add(baseSpecies.toShowdownID())
                }
            }
        }
        destination
    }

    suspend fun isMega(showdownId: ShowdownID) =
        get(showdownId)?.forme?.startsWith("Mega") == true

}

object PokedexTable : Table("pokedex") {
    val id = showdownIDColumn()
    val data = jsonb<Pokemon>("data")

    override val primaryKey = PrimaryKey(id)
}
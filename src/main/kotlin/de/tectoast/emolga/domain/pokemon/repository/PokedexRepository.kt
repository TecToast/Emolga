package de.tectoast.emolga.domain.pokemon.repository

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.pokemon.model.Pokemon
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.model.showdownIDColumn
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.referencesCascade
import de.tectoast.emolga.utils.toShowdownID
import kotlinx.coroutines.flow.associateTo
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class PokedexRepository(private val db: R2dbcDatabase) : StartupTask {
    private val pokedex = mutableMapOf<ShowdownID, Pokemon>()
    private val cosmeticLookup = mutableMapOf<ShowdownID, Pokemon>()
    private val lock = Mutex()

    override suspend fun onStartup() {
        setupCacheIfRequired()
    }

    private suspend fun setupCacheIfRequired() {
        lock.withLock {
            if (pokedex.isEmpty()) {
                suspendTransaction(db) {
                    PokedexTable.selectAll().map { it[PokedexTable.id] to it[PokedexTable.data] }
                        .associateTo(pokedex) { it }
                }
                for (pokemon in pokedex.values) {
                    val formes = pokemon.cosmeticFormes ?: continue
                    for (forme in formes) {
                        cosmeticLookup[forme.toShowdownID()] = pokemon
                    }
                }
            }
        }
    }

    private fun lookup(id: ShowdownID): Pokemon? = cosmeticLookup[id] ?: pokedex[id]

    private suspend inline fun <T> withReady(block: suspend () -> T): T {
        setupCacheIfRequired()
        return block()
    }

    suspend fun getPokedexNumber(showdownId: ShowdownID): Int? = withReady {
        lookup(showdownId)?.num
    }

    suspend fun getPokedexNumbers(showdownIds: Iterable<ShowdownID>): Map<ShowdownID, Int> = withReady {
        val destination = mutableMapOf<ShowdownID, Int>()
        for (id in showdownIds) {
            lookup(id)?.num?.let { destination[id] = it }
        }
        destination
    }

    suspend fun get(id: ShowdownID): Pokemon? = withReady {
        lookup(id)
    }

    suspend fun getAll(ids: Iterable<ShowdownID>): Map<ShowdownID, Pokemon> = withReady {
        val destination = mutableMapOf<ShowdownID, Pokemon>()
        for (id in ids) {
            lookup(id)?.let { destination[id] = it }
        }
        destination
    }

    suspend fun getAllPossibleForms(ids: Iterable<ShowdownID>): Map<ShowdownID, Set<ShowdownID>> = withReady {
        val destination = mutableMapOf<ShowdownID, Set<ShowdownID>>()
        for (id in ids) {
            lookup(id)?.let { pokemon ->
                val formes = pokemon.otherFormes
                val baseSpecies = pokemon.baseSpecies
                destination[id] = buildSet {
                    add(id)
                    add(pokemon.name.toShowdownID())
                    if (formes != null) addAll(formes.map(String::toShowdownID))
                    if (baseSpecies != null) {
                        val baseId = baseSpecies.toShowdownID()
                        add(baseId)
                        val baseOtherForms = pokedex[baseId]?.otherFormes
                        if (baseOtherForms != null) addAll(baseOtherForms.map(String::toShowdownID))
                    }
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

context(t: Table)
fun <C : Column<ShowdownID>> C.referencesPokedex(): C = referencesCascade(PokedexTable.id)
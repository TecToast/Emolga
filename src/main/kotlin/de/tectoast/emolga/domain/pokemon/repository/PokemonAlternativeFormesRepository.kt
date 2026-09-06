package de.tectoast.emolga.domain.pokemon.repository

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.model.showdownIDColumn
import de.tectoast.emolga.utils.referencesCascade
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.koin.core.annotation.Single

@Single
class PokemonAlternativeFormesRepository(private val db: R2dbcDatabase) : StartupTask {
    private val alternativeForms = mutableMapOf<Long, MutableMap<ShowdownID, MutableSet<ShowdownID>>>()
    private var isCacheSetup = false
    private val lock = Mutex()

    override val priority = -1

    override suspend fun onStartup() {
        setupCacheIfRequired()
    }

    suspend fun getAlternativeFormes(guild: Long, baseFormeId: ShowdownID): Set<ShowdownID> {
        setupCacheIfRequired()
        return alternativeForms[guild]?.get(baseFormeId) ?: emptySet()
    }

    private suspend fun setupCacheIfRequired() {
        lock.withLock {
            if (!isCacheSetup) {
                suspendTransaction(db, PokemonAlternativeFormesTable) {
                    selectAll().collect {
                        alternativeForms
                            .getOrPut(it[PokemonAlternativeFormesTable.guild]) { mutableMapOf() }
                            .getOrPut(it[PokemonAlternativeFormesTable.alternativeFormeId]) { mutableSetOf() }
                            .add(it[PokemonAlternativeFormesTable.baseFormeId])
                    }
                }
                isCacheSetup = true
            }
        }
    }
}


object PokemonAlternativeFormesTable : Table("pokemon_alternative_formes") {
    val id = long("id").autoIncrement()
    val guild = long("guild_id")
    val baseFormeId = showdownIDColumn("base_forme").referencesCascade(PokedexTable.id)
    val alternativeFormeId = showdownIDColumn("alternative_forme").referencesCascade(PokedexTable.id)

    override val primaryKey = PrimaryKey(id)
}
package de.tectoast.emolga.domain.league.draft.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.service.core.PicksModifiedFlow
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.database.showdownIDColumn
import de.tectoast.emolga.utils.groupByMapping
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class LeaguePickRepository(private val db: R2dbcDatabase, private val picksModifiedFlow: PicksModifiedFlow) {
    suspend fun getPicksForUser(leagueName: String, userIndex: Int) = suspendTransaction(db) {
        LeaguePickTable.selectAll()
            .where { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq userIndex) }
            .orderBy(LeaguePickTable.pickIndex).map { it.toDraftPokemon() }.toList()
    }

    suspend fun getAllPickedIds(leagueName: String) = suspendTransaction(db) {
        LeaguePickTable.selectAll()
            .where { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.quit eq false) }
            .map { it[LeaguePickTable.showdownId] }.toCollection(mutableSetOf())
    }

    private suspend fun getNextIndex(leagueName: String, userIndex: Int): Int {
        val maxIndexExpr = LeaguePickTable.pickIndex.max()
        val currentMax = LeaguePickTable.select(maxIndexExpr)
            .where { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq userIndex) }
            .firstOrNull()?.get(maxIndexExpr)
        val nextIndex = (currentMax ?: -1) + 1
        return nextIndex
    }

    suspend fun deleteFromLeague(leagueName: String) = suspendTransaction(db) {
        LeaguePickTable.deleteWhere { LeaguePickTable.leagueName eq leagueName }
    }

    suspend fun getAllPicks(leagueName: String) = suspendTransaction(db, LeaguePickTable) {
        LeaguePickTable.selectAll().where { (this.leagueName eq leagueName) }.orderBy(LeaguePickTable.pickIndex)
            .groupByMapping({ it[LeaguePickTable.userIndex] }) { it.toDraftPokemon() }
    }

    suspend fun getAllCurrentPicksInLeagues(leagueNames: Iterable<String>) = suspendTransaction(db, LeaguePickTable) {
        LeaguePickTable.selectAll().where { (this.leagueName inList leagueNames) and (this.quit eq false) }
            .orderBy(LeaguePickTable.pickIndex)
            .groupByMapping({ it[LeaguePickTable.leagueName] }) { it.toDraftPokemon() }
    }

    suspend fun saveNewPick(
        guild: Long,
        leagueName: String,
        userIndex: Int,
        pokemonId: ShowdownID,
        tier: String,
        free: Boolean,
        noCost: Boolean,
        tera: Boolean
    ) = suspendTransaction(db) {
        val nextIndex = getNextIndex(leagueName, userIndex)
        LeaguePickTable.insert {
            it[LeaguePickTable.leagueName] = leagueName
            it[LeaguePickTable.userIndex] = userIndex
            it[LeaguePickTable.pickIndex] = nextIndex
            it[LeaguePickTable.showdownId] = pokemonId
            it[LeaguePickTable.tier] = tier
            it[LeaguePickTable.freePick] = free
            it[LeaguePickTable.noCost] = noCost
            it[LeaguePickTable.tera] = tera
        }
        nextIndex
    }.also { picksModifiedFlow.tryEmit(guild) }


    suspend fun saveSwitch(
        guild: Long,
        leagueName: String,
        userIndex: Int,
        oldPokemonId: ShowdownID,
        newPokemonId: ShowdownID,
        tier: String,
        replace: Boolean
    ) = suspendTransaction(db) {
        if (replace) {
            LeaguePickTable.updateReturning(
                listOf(LeaguePickTable.pickIndex),
                { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq userIndex) and (LeaguePickTable.showdownId eq oldPokemonId) }) {
                it[showdownId] = newPokemonId
            }.firstOrNull()?.get(LeaguePickTable.pickIndex) ?: -1
        } else {
            LeaguePickTable.update({ (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq userIndex) and (LeaguePickTable.showdownId eq oldPokemonId) }) {
                it[quit] = true
            }
            val nextIndex = getNextIndex(leagueName, userIndex)
            LeaguePickTable.insert {
                it[LeaguePickTable.leagueName] = leagueName
                it[LeaguePickTable.userIndex] = userIndex
                it[LeaguePickTable.pickIndex] = nextIndex
                it[LeaguePickTable.showdownId] = newPokemonId
                it[LeaguePickTable.tier] = tier
            }
            nextIndex
        }
    }.also { picksModifiedFlow.tryEmit(guild) }

    suspend fun storeNewPickList(guild: Long, leagueName: String, idx: Int, myPicks: List<DraftPokemon>) {
        suspendTransaction(db, LeaguePickTable) {
            deleteWhere { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq idx) }
            batchInsert(myPicks.withIndex(), shouldReturnGeneratedValues = false) { (index, pokemon) ->
                this[LeaguePickTable.leagueName] = leagueName
                this[LeaguePickTable.userIndex] = idx
                this[LeaguePickTable.pickIndex] = index
                this[LeaguePickTable.showdownId] = pokemon.showdownId
                this[LeaguePickTable.tier] = pokemon.tier
                this[LeaguePickTable.freePick] = pokemon.free
                this[LeaguePickTable.noCost] = pokemon.noCost
                this[LeaguePickTable.tera] = pokemon.tera
            }
        }
        picksModifiedFlow.tryEmit(guild)
    }

    private fun ResultRow.toDraftPokemon() = DraftPokemon(
        showdownId = this[LeaguePickTable.showdownId],
        tier = this[LeaguePickTable.tier],
        free = this[LeaguePickTable.freePick],
        quit = this[LeaguePickTable.quit],
        noCost = this[LeaguePickTable.noCost],
        tera = this[LeaguePickTable.tera]
    )

}


object LeaguePickTable : Table("league_pick") {
    val leagueName = text("league_name").referencesLeagueName()
    val userIndex = integer("user_index")
    val pickIndex = integer("pick_index")
    val showdownId = showdownIDColumn()
    val tier = text("tier")
    val quit = bool("quit").default(false)
    val freePick = bool("free_pick").default(false)
    val noCost = bool("no_cost").default(false)
    val tera = bool("tera").default(false)

    override val primaryKey = PrimaryKey(leagueName, userIndex, pickIndex)

    init {
        index(false, leagueName, userIndex)
    }
}

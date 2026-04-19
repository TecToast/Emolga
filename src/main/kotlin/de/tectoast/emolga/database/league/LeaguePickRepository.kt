package de.tectoast.emolga.database.league

import de.tectoast.emolga.utils.draft.DraftPokemon
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object LeaguePickTable : Table("league_pick") {
    val leagueName = varchar("league_name", 255).references(LeagueCoreTable.leagueName)
    val userIndex = integer("user_index")
    val pickIndex = integer("pick_index")
    val showdownId = varchar("mon_name", 255)
    val tier = varchar("tier", 64)
    val quit = bool("quit").default(false)
    val freePick = bool("free_pick").default(false)
    val noCost = bool("no_cost").default(false)
    val tera = bool("tera").default(false)

    override val primaryKey = PrimaryKey(leagueName, userIndex, pickIndex)

    init {
        index(false, leagueName, userIndex)
    }
}

class LeaguePickRepository(val db: R2dbcDatabase) {
    suspend fun getPicksForUser(leagueName: String, userIndex: Int) = suspendTransaction(db) {
        LeaguePickTable.selectAll()
            .where { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq userIndex) }
            .orderBy(LeaguePickTable.pickIndex)
            .map {
                DraftPokemon(
                    name = it[LeaguePickTable.showdownId],
                    tier = it[LeaguePickTable.tier],
                    free = it[LeaguePickTable.freePick],
                    quit = it[LeaguePickTable.quit],
                    noCost = it[LeaguePickTable.noCost],
                    tera = it[LeaguePickTable.tera]
                )
            }
            .toList()
    }

    suspend fun getAllPickedIds(leagueName: String) = suspendTransaction(db) {
        LeaguePickTable.selectAll()
            .where { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.quit eq false) }
            .map { it[LeaguePickTable.showdownId] }
            .toCollection(mutableSetOf())
    }

    suspend fun saveNewPick(leagueName: String, userIndex: Int, pokemonId: String, tier: String, free: Boolean, noCost: Boolean, tera: Boolean) = suspendTransaction(db) {
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
    }


    suspend fun saveSwitch(leagueName: String, userIndex: Int, oldPokemonId: String, newPokemonId: String, tier: String, replace: Boolean) = suspendTransaction(db) {
        if(replace) {
            LeaguePickTable.updateReturning(listOf(LeaguePickTable.pickIndex),{ (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq userIndex) and (LeaguePickTable.showdownId eq oldPokemonId) }) {
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
    }
    private suspend fun getNextIndex(leagueName: String, userIndex: Int): Int {
        val maxIndexExpr = LeaguePickTable.pickIndex.max()
        val currentMax = LeaguePickTable.select(maxIndexExpr)
            .where { (LeaguePickTable.leagueName eq leagueName) and (LeaguePickTable.userIndex eq userIndex) }
            .firstOrNull()?.get(maxIndexExpr)
        val nextIndex = (currentMax ?: -1) + 1
        return nextIndex
    }
}


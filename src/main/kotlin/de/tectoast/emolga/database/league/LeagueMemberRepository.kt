package de.tectoast.emolga.database.league

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object LeagueUserTable : Table("league_user") {
    val leagueName = varchar("league_name", 255).references(LeagueCoreTable.leagueName)
    val userIndex = integer("user_index")
    val userOrder = integer("user_order").default(0)
    val userId = long("user_id")
    val substitute = bool("substitute").default(false)
    val shouldPing = bool("should_ping").default(true)

    override val primaryKey = PrimaryKey(leagueName, userIndex, userOrder)

    init {
        index(false, userId)
        index(false, leagueName, userIndex)
    }
}

class LeagueMemberRepository(val db: R2dbcDatabase) {
    suspend fun getIdxOfParticipant(leagueName: String, userId: Long) = suspendTransaction(db) {
        LeagueUserTable.select(LeagueUserTable.userIndex)
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.userId eq userId) and (LeagueUserTable.substitute eq false) }
            .firstOrNull()?.get(LeagueUserTable.userIndex)
    }

    suspend fun getSingleParticipantAsSubstitute(leagueName: String, userId: Long) = suspendTransaction(db) {
        LeagueUserTable.select(LeagueUserTable.userIndex, LeagueUserTable.substitute)
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.userId eq userId) }.singleOrNull()
            ?.takeIf { it[LeagueUserTable.substitute] }?.get(LeagueUserTable.userIndex)
    }

    suspend fun isAuthorizedFor(leagueName: String, idx: Int, uid: Long) = suspendTransaction(db) {
        LeagueUserTable.selectAll()
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.userIndex eq idx) and (LeagueUserTable.userId eq uid) }
            .count() > 0
    }

    suspend fun getAllIdsToPing(leagueName: String) = suspendTransaction(db) {
        LeagueUserTable.select(LeagueUserTable.userId)
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.shouldPing eq true) }.toList()
            .groupBy { it[LeagueUserTable.userIndex] }
            .mapValues { entry -> entry.value.map { it[LeagueUserTable.userId] } }
    }

    suspend fun getPrimaryIds(leagueName: String) = suspendTransaction(db) {
        LeagueUserTable.select(LeagueUserTable.userId)
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.substitute eq false) }
            .orderBy(LeagueUserTable.userIndex).map { it[LeagueUserTable.userId] }.toList()
    }

    suspend fun getParticipantsForIdx(leagueName: String, idx: Int) = suspendTransaction(db) {
        LeagueUserTable.select(LeagueUserTable.userId, LeagueUserTable.substitute, LeagueUserTable.shouldPing)
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.userIndex eq idx) }
            .orderBy(LeagueUserTable.userIndex).map {
                LeagueParticipant(
                    idx = idx,
                    userId = it[LeagueUserTable.userId],
                    substitute = it[LeagueUserTable.substitute],
                    shouldPing = it[LeagueUserTable.shouldPing]
                )
            }.toList()
    }
}

data class LeagueParticipant(val idx: Int, val userId: Long, val substitute: Boolean, val shouldPing: Boolean)
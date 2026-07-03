package de.tectoast.emolga.domain.league.member.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.member.model.LeagueParticipant
import de.tectoast.emolga.utils.groupByMapping
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class LeagueMemberRepository(private val db: R2dbcDatabase) {
    suspend fun getIdxOfParticipant(leagueName: String, userId: Long) = suspendTransaction(db, LeagueUserTable) {
        select(idx)
            .where { (this.leagueName eq leagueName) and (this.userId eq userId) and (this.substitute eq false) }
            .firstOrNull()?.get(idx)
    }

    suspend fun getSingleParticipantAsSubstitute(leagueName: String, userId: Long) =
        suspendTransaction(db, LeagueUserTable) {
            select(idx, substitute)
                .where { (this.leagueName eq leagueName) and (this.userId eq userId) }.singleOrNull()
                ?.takeIf { it[substitute] }?.get(idx)
        }

    suspend fun isAuthorizedFor(leagueName: String, idx: Int, uid: Long) = suspendTransaction(db, LeagueUserTable) {
        selectAll()
            .where { (this.leagueName eq leagueName) and (this.idx eq idx) and (this.userId eq uid) }
            .count() > 0
    }

    suspend fun getAllIdsToPing(leagueName: String) = suspendTransaction(db, LeagueUserTable) {
        select(idx, userId)
            .where { (this.leagueName eq leagueName) and (this.shouldPing eq true) }
            .orderBy(userOrder)
            .groupByMapping({ it[LeagueUserTable.idx] }) { it[LeagueUserTable.userId] }
    }

    suspend fun getPrimaryIds(leagueName: String) = suspendTransaction(db, LeagueUserTable) {
        select(idx, userId)
            .where { (this.leagueName eq leagueName) and (this.substitute eq false) }
            .orderBy(this.userOrder)
            .groupByMapping({ it[LeagueUserTable.idx] }) { it[LeagueUserTable.userId] }
    }

    suspend fun getPrimaryIds(leagueName: String, indices: Iterable<Int>) = suspendTransaction(db, LeagueUserTable) {
        select(idx, userId)
            .where { (this.leagueName eq leagueName) and (this.substitute eq false) and (this.idx inList indices) }
            .orderBy(this.userOrder)
            .groupByMapping({ it[LeagueUserTable.idx] }) { it[LeagueUserTable.userId] }
    }

    suspend fun getPrimaryIds(leagueName: String, idx: Int) = suspendTransaction(db, LeagueUserTable) {
        select(userId)
            .where { (this.leagueName eq leagueName) and (this.substitute eq false) and (this.idx eq idx) }
            .orderBy(this.userOrder)
            .map { it[userId] }.toList()
    }

    suspend fun getParticipantsForIdx(leagueName: String, idx: Int) = getParticipants { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.idx eq idx) }

    suspend fun getAllParticipants(leagueName: String) = getParticipants { LeagueUserTable.leagueName eq leagueName }

    private suspend fun getParticipants(condition: () -> Op<Boolean>) = suspendTransaction(db, LeagueUserTable) {
        select(idx, userId, substitute, shouldPing)
            .where(condition)
            .orderBy(this.idx to SortOrder.ASC, this.userOrder to SortOrder.ASC).map {
                LeagueParticipant(
                    idx = it[idx],
                    userId = it[userId],
                    substitute = it[substitute],
                    shouldPing = it[shouldPing]
                )
            }.toList()
    }



    suspend fun setPrimaryUsers(leagueName: String, users: List<Long>) = suspendTransaction(db, LeagueUserTable) {
        deleteWhere { this.leagueName eq leagueName and (substitute eq false) }
        batchInsert(users.withIndex()) { (idx, userId) ->
            this[LeagueUserTable.leagueName] = leagueName
            this[LeagueUserTable.idx] = idx
            this[LeagueUserTable.userId] = userId
        }
    }

    suspend fun setTeammates(leagueName: String, users: List<Long>) = suspendTransaction(db) {
        LeagueUserTable.deleteWhere {
            (LeagueUserTable.leagueName eq leagueName) and
                    ((LeagueUserTable.substitute eq true) or (LeagueUserTable.userOrder greater 0))
        }
        val maxExpr = LeagueUserTable.userOrder.max()
        val currentMaxValues = LeagueUserTable.select(LeagueUserTable.idx, maxExpr)
            .where { LeagueUserTable.leagueName eq leagueName }
            .groupBy(LeagueUserTable.idx)
            .associate {
                it[LeagueUserTable.idx] to (it[maxExpr] ?: 0)
            }
        LeagueUserTable.batchInsert(users.withIndex()) { (idx, userId) ->
            this[LeagueUserTable.leagueName] = leagueName
            this[LeagueUserTable.idx] = idx
            this[LeagueUserTable.userId] = userId
            this[LeagueUserTable.userOrder] = (currentMaxValues[idx] ?: 0) + 1
        }
    }

    suspend fun addUser(leagueName: String, idx: Int, userId: Long, substitute: Boolean, shouldPing: Boolean) =
        suspendTransaction(db) {
            val nextUserOrder = getNextUserOrder(leagueName, idx)
            LeagueUserTable.upsert {
                it[LeagueUserTable.leagueName] = leagueName
                it[LeagueUserTable.idx] = idx
                it[LeagueUserTable.userId] = userId
                it[LeagueUserTable.userOrder] = nextUserOrder
                it[LeagueUserTable.substitute] = substitute
                it[LeagueUserTable.shouldPing] = shouldPing
            }
        }

    suspend fun removeUser(leagueName: String, idx: Int, userId: Long) = suspendTransaction(db) {
        LeagueUserTable.deleteWhere { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.idx eq idx) and (LeagueUserTable.userId eq userId) }
    }

    suspend fun replaceUser(leagueName: String, oldUserId: Long, newUserId: Long) = suspendTransaction(db) {
        LeagueUserTable.update({ (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.substitute eq false) and (LeagueUserTable.userId eq oldUserId) }) {
            it[LeagueUserTable.userId] = newUserId
        }
    }

    suspend fun modifyUserPing(leagueName: String, idx: Int, user: Long, shouldPing: Boolean) = suspendTransaction(db) {
        LeagueUserTable.update({ (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.idx eq idx) and (LeagueUserTable.userId eq user) }) {
            it[LeagueUserTable.shouldPing] = shouldPing
        }
    }

    suspend fun isPrimaryUser(leagueName: String, idx: Int, userId: Long) = suspendTransaction(db, LeagueUserTable) {
        selectAll().where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.idx eq idx) and (LeagueUserTable.userId eq userId) and (LeagueUserTable.substitute eq false) }
            .count() > 0
    }

    suspend fun getParticipantSize(leagueName: String) = suspendTransaction(db, LeagueUserTable) {
        val count = LeagueUserTable.idx.countDistinct()
        select(count).where { this.leagueName eq leagueName }.first()[count].toInt()
    }

    private suspend fun getNextUserOrder(leagueName: String, idx: Int): Int {
        val maxExpr = LeagueUserTable.userOrder.max()
        return (LeagueUserTable.select(maxExpr)
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.idx eq idx) }.firstOrNull()
            ?.get(maxExpr) ?: 0) + 1
    }


}

object LeagueUserTable : Table("league_user") {
    val leagueName = text("league_name").referencesLeagueName()
    val idx = integer("user_index")
    val userOrder = integer("user_order").default(0)
    val userId = long("user_id")
    val substitute = bool("substitute").default(false)
    val shouldPing = bool("should_ping").default(true)

    override val primaryKey = PrimaryKey(leagueName, idx, userOrder)

    init {
        index(false, userId)
        index(false, leagueName, idx)
    }
}

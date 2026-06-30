package de.tectoast.emolga.domain.league.queue.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.queue.model.QueuePicksUserData
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


@Single
class QueuedPicksRepository(private val db: R2dbcDatabase) {
    suspend fun getForLeague(leagueName: String): Map<Int, QueuePicksUserData> = suspendTransaction(db) {
        QueuedPicksTable.select(QueuedPicksTable.idx, QueuedPicksTable.data)
            .where { (QueuedPicksTable.leagueName eq leagueName) }
            .associate { it[QueuedPicksTable.idx] to it[QueuedPicksTable.data] }
    }

    suspend fun getSingle(leagueName: String, idx: Int): QueuePicksUserData = suspendTransaction(db) {
        QueuedPicksTable.select(QueuedPicksTable.data)
            .where { (QueuedPicksTable.leagueName eq leagueName) and (QueuedPicksTable.idx eq idx) }
            .firstOrNull()?.get(QueuedPicksTable.data) ?: QueuePicksUserData()
    }

    suspend fun updateForLeague(leagueName: String, data: Map<Int, QueuePicksUserData>) = suspendTransaction(db) {
        data.forEach { (idx, userData) ->
            QueuedPicksTable.upsert {
                it[QueuedPicksTable.leagueName] = leagueName
                it[QueuedPicksTable.idx] = idx
                it[QueuedPicksTable.data] = userData
            }
        }
    }

    suspend fun updateSingle(leagueName: String, idx: Int, data: QueuePicksUserData) = suspendTransaction(db) {
        QueuedPicksTable.upsert {
            it[QueuedPicksTable.leagueName] = leagueName
            it[QueuedPicksTable.idx] = idx
            it[QueuedPicksTable.data] = data
        }
    }

}

object QueuedPicksTable : Table("queued_picks") {
    val leagueName = text("leaguename").referencesLeagueName()
    val idx = integer("idx")
    val data = jsonb<QueuePicksUserData>("data")

    override val primaryKey = PrimaryKey(leagueName, idx)
}
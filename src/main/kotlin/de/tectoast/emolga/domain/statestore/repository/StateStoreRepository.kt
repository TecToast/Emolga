package de.tectoast.emolga.domain.statestore.repository

import de.tectoast.emolga.di.CleanupTask
import de.tectoast.emolga.domain.statestore.model.StateStore
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant


@Single
class StateStoreRepository(private val db: R2dbcDatabase, private val clock: Clock) : CleanupTask {
    suspend fun save(uid: Long, state: StateStore) = suspendTransaction(db) {
        StateStoreTable.upsert {
            it[StateStoreTable.uid] = uid
            it[StateStoreTable.type] = state::class.simpleName!!
            it[StateStoreTable.data] = state
            it[StateStoreTable.timestamp] = clock.now()
        }
    }

    suspend fun delete(uid: Long, state: StateStore) = suspendTransaction(db) {
        StateStoreTable.deleteWhere {
            (StateStoreTable.uid eq uid) and (StateStoreTable.type eq state::class.simpleName!!)
        }
    }

    suspend fun get(uid: Long, type: String): StateStore? = suspendTransaction(db) {
        StateStoreTable.selectAll().where { (StateStoreTable.uid eq uid) and (StateStoreTable.type eq type) }
            .map { it[StateStoreTable.data] }.firstOrNull()
    }

    override suspend fun cleanup(now: Instant) {
        suspendTransaction(db) {
            StateStoreTable.deleteWhere { StateStoreTable.timestamp lessEq now.minus(7.days) }
        }
    }
}

object StateStoreTable : Table("state_store") {
    val uid = long("user")
    val type = text("type")
    val data = jsonb<StateStore>("data")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(uid, type)
}
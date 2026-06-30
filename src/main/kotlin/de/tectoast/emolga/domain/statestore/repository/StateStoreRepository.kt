package de.tectoast.emolga.domain.statestore.repository

import de.tectoast.emolga.domain.statestore.model.StateStore
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


@Single
class StateStoreRepository(private val db: R2dbcDatabase) {
    suspend fun save(uid: Long, state: StateStore) {
        StateStoreTable.upsert {
            it[StateStoreTable.uid] = uid
            it[StateStoreTable.type] = state::class.simpleName!!
            it[StateStoreTable.data] = state
        }
    }

    suspend fun delete(uid: Long, state: StateStore) {
        StateStoreTable.deleteWhere {
            (StateStoreTable.uid eq uid) and (StateStoreTable.type eq state::class.simpleName!!)
        }
    }

    suspend fun get(uid: Long, type: String): StateStore? {
        return StateStoreTable.selectAll().where { (StateStoreTable.uid eq uid) and (StateStoreTable.type eq type) }
            .map { it[StateStoreTable.data] }.firstOrNull()
    }
}

object StateStoreTable : Table("state_store") {
    val uid = long("user")
    val type = text("type")
    val data = jsonb<StateStore>("data")

    override val primaryKey = PrimaryKey(uid, type)
}
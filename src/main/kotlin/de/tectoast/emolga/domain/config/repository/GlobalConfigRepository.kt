package de.tectoast.emolga.domain.config.repository

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single


private const val MAINTENANCE_KEY = "maintenance_mode"

@Single
class GlobalConfigRepository(private val db: R2dbcDatabase) {

    suspend fun setMaintenanceMode(maintenance: String?) = suspendTransaction(db) {
        GlobalConfigTable.upsert {
            it[key] = MAINTENANCE_KEY
            it[value] = maintenance
        }
    }

    suspend fun getMaintenanceMode() = suspendTransaction(db) {
        GlobalConfigTable.select(GlobalConfigTable.value).where { GlobalConfigTable.key eq MAINTENANCE_KEY }
            .map { it[GlobalConfigTable.value] }
            .firstOrNull()
    }
}

object GlobalConfigTable : Table("global_config") {
    val key = text("key")
    val value = text("value").nullable()

    override val primaryKey = PrimaryKey(key)
}

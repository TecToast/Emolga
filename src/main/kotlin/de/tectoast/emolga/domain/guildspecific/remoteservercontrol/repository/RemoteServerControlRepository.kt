package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.repository

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlConfig
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlData
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class RemoteServerControlRepository(private val db: R2dbcDatabase) {
    suspend fun getByName(name: String) = suspendTransaction(db) {
        RemoteServerControlTable.select(RemoteServerControlTable.data)
            .where { RemoteServerControlTable.name eq name }
            .firstOrNull()
            ?.let { it[RemoteServerControlTable.data] }
    }

    suspend fun getAll() = suspendTransaction(db) {
        RemoteServerControlTable.selectAll()
            .map { RemoteServerControlData(it[RemoteServerControlTable.name], it[RemoteServerControlTable.data]) }
            .toList()
    }
}

object RemoteServerControlTable : Table("remote_server_control") {
    val name = text("name")
    val data = jsonb<RemoteServerControlConfig>("data")
}
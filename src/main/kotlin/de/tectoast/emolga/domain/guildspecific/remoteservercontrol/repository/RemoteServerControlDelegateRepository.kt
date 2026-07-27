package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.repository

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlAction
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlActionData
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.koin.core.annotation.Single

@Single
class RemoteServerControlDelegateRepository(private val db: R2dbcDatabase) {
    suspend fun getData(id: Int) = suspendTransaction(db, RemoteServerControlDelegateTable) {
        select(pc, action).where { this.id eq id }.map { RemoteServerControlActionData(it[this.pc], it[this.action]) }
            .firstOrNull()
    }
}

object RemoteServerControlDelegateTable : Table("remote_server_control_delegate") {
    val id = integer("id").autoIncrement()
    val pc = text("pc")
    val action = enumerationByName<RemoteServerControlAction>("action", 64)
    val note = text("note").nullable()

    override val primaryKey = PrimaryKey(id)
}
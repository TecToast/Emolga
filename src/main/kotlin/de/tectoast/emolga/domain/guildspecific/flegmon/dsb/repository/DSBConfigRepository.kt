package de.tectoast.emolga.domain.guildspecific.flegmon.dsb.repository

import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.model.DSBConfig
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class DSBConfigRepository(private val db: R2dbcDatabase) {
    suspend fun getDSBConfig(host: Long) = suspendTransaction(db) {
        DSBConfigTable.selectAll()
            .where { DSBConfigTable.host eq host }
            .map {
                DSBConfig(
                    host = it[DSBConfigTable.host],
                    guild = it[DSBConfigTable.guild],
                    categories = it[DSBConfigTable.categories],
                    users = it[DSBConfigTable.users]
                )
            }
            .firstOrNull()
    }
}

object DSBConfigTable : Table("dsb_config") {
    val host = long("host")
    val guild = long("guild")
    val categories = array<String>("categories")
    val users = array<Long>("users")

    override val primaryKey = PrimaryKey(host)
}
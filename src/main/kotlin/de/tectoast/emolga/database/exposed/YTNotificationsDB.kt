package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

object YTNotificationsTable : Table("ytnotifications") {
    val discordChannel = long("dcchannel")
    val ytChannel = varchar("ytchannel", 31)
    val dm = bool("dm").default(false)

    override val primaryKey = PrimaryKey(discordChannel, ytChannel)

}

@Single
class YTNotificationsRepository(val db: R2dbcDatabase) {

    suspend fun addData(id: Long, dm: Boolean, data: Iterable<String>) {
        suspendTransaction(db) {
            YTNotificationsTable.batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
                this[YTNotificationsTable.ytChannel] = it
                this[YTNotificationsTable.discordChannel] = id
                this[YTNotificationsTable.dm] = dm
            }
        }
    }

    suspend fun getAllYTChannels() = suspendTransaction(db) {
        YTNotificationsTable.select(YTNotificationsTable.ytChannel).withDistinct(true)
            .map { it[YTNotificationsTable.ytChannel] }
            .toCollection(mutableSetOf())
    }

    suspend fun getDCChannels(ytChannel: String) = suspendTransaction(db) {
        YTNotificationsTable.select(YTNotificationsTable.discordChannel, YTNotificationsTable.dm)
            .where { YTNotificationsTable.ytChannel eq ytChannel }
            .map { it[YTNotificationsTable.discordChannel] to it[YTNotificationsTable.dm] }.toList()
    }
}
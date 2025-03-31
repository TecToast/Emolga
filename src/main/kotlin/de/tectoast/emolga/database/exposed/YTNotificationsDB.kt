package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object YTNotificationsDB : Table("ytnotifications") {
    val DCCHANNEL = long("dcchannel")
    val YTCHANNEL = varchar("ytchannel", 31)
    val DM = bool("dm").default(false)

    suspend fun addData(id: Long, dm: Boolean, data: Iterable<String>) = newSuspendedTransaction {
        batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
            this[YTCHANNEL] = it
            this[DCCHANNEL] = id
            this[DM] = dm
        }
    }

    suspend fun getAllYTChannels() = newSuspendedTransaction {
        select(YTCHANNEL).withDistinct(true).mapTo(mutableSetOf()) { it[YTCHANNEL] }
    }

    suspend fun getDCChannels(ytChannel: String) = newSuspendedTransaction {
        select(DCCHANNEL).where { YTCHANNEL eq ytChannel }.map { it[DCCHANNEL] to it[DM] }
    }
}

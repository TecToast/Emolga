package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object YTNotificationsDB : Table("ytnotifications") {
    val DCCHANNEL = long("dcchannel")
    val YTCHANNEL = varchar("ytchannel", 31)
    val DM = bool("dm").default(false)

    suspend fun getAllYTChannels() = newSuspendedTransaction {
        select(YTCHANNEL).withDistinct(true).mapTo(mutableSetOf()) { it[YTCHANNEL] }
    }

    suspend fun getDCChannel(ytChannel: String) = newSuspendedTransaction {
        select(DCCHANNEL).where { YTCHANNEL eq ytChannel }.firstOrNull()?.let { it[DCCHANNEL] to it[DM] }
    }
}

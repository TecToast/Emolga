package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert

object YTNotificationsDB : Table("ytnotifications") {
    val DCCHANNEL = long("dcchannel")
    val YTCHANNEL = varchar("ytchannel", 31)
    val DM = bool("dm").default(false)

    suspend fun addData(id: Long, dm: Boolean, data: Iterable<String>) = dbTransaction {
        batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
            this[YTCHANNEL] = it
            this[DCCHANNEL] = id
            this[DM] = dm
        }
    }

    suspend fun getAllYTChannels() = dbTransaction {
        select(YTCHANNEL).withDistinct(true).mapTo(mutableSetOf()) { it[YTCHANNEL] }
    }

    suspend fun getDCChannels(ytChannel: String) = dbTransaction {
        select(DCCHANNEL, DM).where { YTCHANNEL eq ytChannel }.map { it[DCCHANNEL] to it[DM] }
    }
}

package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select

object YTNotificationsDB : Table("ytnotifications") {
    val DCCHANNEL = long("dcchannel")
    val YTCHANNEL = varchar("ytchannel", 31)
    val DM = bool("dm").default(false)

    override val primaryKey = PrimaryKey(DCCHANNEL, YTCHANNEL)

    suspend fun addData(id: Long, dm: Boolean, data: Iterable<String>) = dbTransaction {
        batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
            this[YTCHANNEL] = it
            this[DCCHANNEL] = id
            this[DM] = dm
        }
    }

    suspend fun getAllYTChannels() = dbTransaction {
        select(YTCHANNEL).withDistinct(true).map { it[YTCHANNEL] }.toCollection(mutableSetOf())
    }

    suspend fun getDCChannels(ytChannel: String) = dbTransaction {
        select(DCCHANNEL, DM).where { YTCHANNEL eq ytChannel }.map { it[DCCHANNEL] to it[DM] }.toList()
    }
}

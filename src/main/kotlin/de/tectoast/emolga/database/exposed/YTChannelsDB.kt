package de.tectoast.emolga.database.exposed

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object YTChannelsDB : Table("ytchannels") {
    val USER = long("user")
    val CHANNELID = varchar("channelid", 31)

    suspend fun getUserByChannelId(channelId: String) = newSuspendedTransaction {
        select(USER).where { CHANNELID eq channelId }.firstOrNull()?.let { it[USER] }
    }

    suspend fun addAllChannelIdsToSet(set: MutableSet<String>, users: Iterable<Long>) = newSuspendedTransaction {
        select(CHANNELID).where { USER inList users }.forEach { set += it[CHANNELID] }
    }

    suspend fun insertAll(data: List<Pair<Long, String>>) = newSuspendedTransaction {
        batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
            this[USER] = it.first
            this[CHANNELID] = it.second
        }
    }
}
package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert

object YTChannelsDB : Table("ytchannels") {
    val USER = long("user")
    val CHANNELID = varchar("channelid", 31)

    override val primaryKey = PrimaryKey(USER, CHANNELID)

    suspend fun getUsersByChannelId(channelId: String) = dbTransaction {
        select(USER).where { CHANNELID eq channelId }.map { it[USER] }
    }

    suspend fun addAllChannelIdsToSet(set: MutableSet<String>, users: Iterable<Long>) = dbTransaction {
        select(CHANNELID).where { USER inList users }.forEach { set += it[CHANNELID] }
    }

    suspend fun insertAll(data: List<Pair<Long, String>>) = dbTransaction {
        batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
            this[USER] = it.first
            this[CHANNELID] = it.second
        }
    }
}
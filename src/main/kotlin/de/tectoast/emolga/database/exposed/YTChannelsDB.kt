package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import kotlinx.coroutines.flow.asFlow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select

object YTChannelsDB : Table("ytchannels") {
    val USER = long("user")
    val CHANNELID = varchar("channelid", 31)

    override val primaryKey = PrimaryKey(USER, CHANNELID)

    suspend fun getUsersByChannelId(channelId: String) = dbTransaction {
        select(USER).where { CHANNELID eq channelId }.map { it[USER] }.toList()
    }

    suspend fun getChannelsOfUser(userId: Long) = dbTransaction {
        select(CHANNELID).where { USER eq userId }.map { it[CHANNELID] }.toList()
    }

    suspend fun addAllChannelIdsToSet(set: MutableSet<String>, users: Iterable<Long>) = dbTransaction {
        select(CHANNELID).where { USER inList users }.asFlow().collect { set += it[CHANNELID] }
    }

    suspend fun insertAll(data: List<Pair<Long, String>>) = dbTransaction {
        batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
            this[USER] = it.first
            this[CHANNELID] = it.second
        }
    }
}

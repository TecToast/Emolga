package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select

object YTChannelsDB : Table("ytchannels") {
    val USER = long("user")
    val CHANNELID = varchar("channelid", 31)
    val HANDLE = varchar("handle", 31).nullable()

    override val primaryKey = PrimaryKey(USER, CHANNELID)

    suspend fun getUsersByChannelId(channelId: String) = dbTransaction {
        select(USER).where { CHANNELID eq channelId }.map { it[USER] }.toList()
    }

    suspend fun getChannelsOfUser(userId: Long) = dbTransaction {
        select(CHANNELID).where { USER eq userId }.map { it[CHANNELID] }.toList()
    }

    suspend fun addAllChannelIdsToSet(set: MutableSet<String>, users: Iterable<Long>) = dbTransaction {
        select(CHANNELID).where { USER inList users }.collect { set += it[CHANNELID] }
    }

    suspend fun insertSingle(user: Long, data: Pair<String, String?>) = dbTransaction {
        insertIgnore {
            it[USER] = user
            it[CHANNELID] = data.first
            it[HANDLE] = data.second
        }
    }

    suspend fun insertAll(data: List<Triple<Long, String, String?>>) = dbTransaction {
        batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
            this[USER] = it.first
            this[CHANNELID] = it.second
            this[HANDLE] = it.third
        }
    }
}

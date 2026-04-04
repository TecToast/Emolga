package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

object YTChannelsTable : Table("ytchannels") {
    val user = long("user")
    val channelid = varchar("channelid", 31)
    val handle = varchar("handle", 31).nullable()

    override val primaryKey = PrimaryKey(user, channelid)
}

@Single
class YTChannelsRepository(val db: R2dbcDatabase) {
    suspend fun getUsersByChannelId(channelId: String) = suspendTransaction(db) {
        YTChannelsTable.select(YTChannelsTable.user).where { YTChannelsTable.channelid eq channelId }
            .map { it[YTChannelsTable.user] }
            .toList()
    }

    suspend fun getChannelsOfUser(userId: Long) = suspendTransaction(db) {
        YTChannelsTable.select(YTChannelsTable.channelid).where { YTChannelsTable.user eq userId }
            .map { it[YTChannelsTable.channelid] }
            .toList()
    }

    suspend fun addAllChannelIdsToSet(set: MutableSet<String>, users: Iterable<Long>) =
        suspendTransaction(db) {
            YTChannelsTable.select(YTChannelsTable.channelid).where { YTChannelsTable.user inList users }
                .collect { set += it[YTChannelsTable.channelid] }
        }

    suspend fun insertSingle(user: Long, data: Pair<String, String?>) {
        suspendTransaction(db) {
            YTChannelsTable.insertIgnore {
                it[this.user] = user
                it[channelid] = data.first
                it[handle] = data.second
            }
        }
    }

    suspend fun insertAll(data: List<Triple<Long, String, String?>>) {
        suspendTransaction(db) {
            YTChannelsTable.batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
                this[YTChannelsTable.user] = it.first
                this[YTChannelsTable.channelid] = it.second
                this[YTChannelsTable.handle] = it.third
            }
        }
    }
}

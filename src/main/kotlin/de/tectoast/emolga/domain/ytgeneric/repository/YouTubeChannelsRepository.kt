package de.tectoast.emolga.domain.ytgeneric.repository

import de.tectoast.emolga.domain.ytgeneric.model.ChannelIdResult
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class YouTubeChannelsRepository(private val db: R2dbcDatabase) {
    suspend fun getUsersByChannelId(channelId: String) = suspendTransaction(db) {
        YTChannelsTable.select(YTChannelsTable.user).where { YTChannelsTable.channelId eq channelId }
            .map { it[YTChannelsTable.user] }
            .toList()
    }

    suspend fun getChannelsOfUser(userId: Long) = suspendTransaction(db) {
        YTChannelsTable.select(YTChannelsTable.channelId).where { YTChannelsTable.user eq userId }
            .map { it[YTChannelsTable.channelId] }
            .toList()
    }

    suspend fun getAllChannelIds(users: Iterable<Long>) =
        suspendTransaction(db) {
            YTChannelsTable.select(YTChannelsTable.channelId).where { YTChannelsTable.user inList users }
                .map { it[YTChannelsTable.channelId] }
                .toSet()
        }

    suspend fun insertSingle(user: Long, data: ChannelIdResult) {
        suspendTransaction(db) {
            YTChannelsTable.insertIgnore {
                it[this.user] = user
                it[channelId] = data.channelId
                it[handle] = data.handle
            }
        }
    }

    suspend fun insertAll(data: List<Triple<Long, String, String?>>) {
        suspendTransaction(db) {
            YTChannelsTable.batchInsert(data, ignore = true, shouldReturnGeneratedValues = false) {
                this[YTChannelsTable.user] = it.first
                this[YTChannelsTable.channelId] = it.second
                this[YTChannelsTable.handle] = it.third
            }
        }
    }
}

object YTChannelsTable : Table("ytchannels") {
    val user = long("user")
    val channelId = text("channel_id")
    val handle = text("handle").nullable()

    override val primaryKey = PrimaryKey(user, channelId)
}
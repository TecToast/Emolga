package de.tectoast.emolga.domain.userdata.repository

import de.tectoast.emolga.di.CleanupTask
import de.tectoast.emolga.discord.DiscordUserData
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant


private val CACHE_TTL = 7.days

@Single
class DiscordUserCacheRepository(private val db: R2dbcDatabase, private val clock: Clock) :
    CleanupTask {
    override suspend fun cleanup(now: Instant) {
        suspendTransaction(db) {
            DiscordUserCacheTable.deleteWhere { DiscordUserCacheTable.lastUpdated lessEq now - CACHE_TTL }
        }
    }

    suspend fun set(userId: Long, name: String, avatar: String) {
        suspendTransaction(db) {
            DiscordUserCacheTable.upsert {
                it[DiscordUserCacheTable.userId] = userId
                it[DiscordUserCacheTable.name] = name
                it[DiscordUserCacheTable.avatar] = avatar
                it[DiscordUserCacheTable.lastUpdated] = clock.now()
            }
        }
    }

    suspend fun set(datas: Map<Long, DiscordUserData>) {
        val now = clock.now()
        suspendTransaction(db) {
            DiscordUserCacheTable.batchUpsert(datas.entries, shouldReturnGeneratedValues = false) {
                this[DiscordUserCacheTable.userId] = it.key
                this[DiscordUserCacheTable.name] = it.value.displayName
                this[DiscordUserCacheTable.avatar] = it.value.avatarUrl
                this[DiscordUserCacheTable.lastUpdated] = now
            }
        }
    }

    suspend fun getValidEntries(users: Iterable<Long>) = suspendTransaction(db) {
        DiscordUserCacheTable.select(
            DiscordUserCacheTable.userId,
            DiscordUserCacheTable.name,
            DiscordUserCacheTable.avatar,
            DiscordUserCacheTable.lastUpdated
        )
            .where { DiscordUserCacheTable.userId inList users and (DiscordUserCacheTable.lastUpdated greaterEq (clock.now() - CACHE_TTL)) }
            .map {
                DiscordUserData(
                    it[DiscordUserCacheTable.userId],
                    it[DiscordUserCacheTable.name],
                    it[DiscordUserCacheTable.avatar]
                )
            }
            .toList()
    }
}

object DiscordUserCacheTable : Table("discord_user_cache") {
    val userId = long("user_id")
    val name = text("name")
    val avatar = text("avatar")
    val lastUpdated = timestamp("last_updated")

    override val primaryKey = PrimaryKey(userId)
}



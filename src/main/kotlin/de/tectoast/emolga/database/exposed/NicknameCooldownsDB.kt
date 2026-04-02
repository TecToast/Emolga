package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface NicknameCooldownsRepository {
    suspend fun getCooldown(guild: Long, user: Long): Instant?
    suspend fun setCooldown(guild: Long, user: Long, until: Instant)
}

@OptIn(ExperimentalTime::class)
@Single(binds = [NicknameCooldownsRepository::class])
class PostgresNicknameCooldownsRepository(val db: R2dbcDatabase, val nicknameCooldowns: NicknameCooldownsDB) :
    NicknameCooldownsRepository {
    override suspend fun getCooldown(guild: Long, user: Long) = suspendTransaction(db) {
        nicknameCooldowns.select(nicknameCooldowns.timestamp).where {
            (nicknameCooldowns.guild eq guild) and (nicknameCooldowns.user eq user) and (nicknameCooldowns.timestamp greaterEq CurrentTimestamp)
        }.firstOrNull()?.get(nicknameCooldowns.timestamp)
    }

    override suspend fun setCooldown(guild: Long, user: Long, until: Instant) {
        suspendTransaction(db) {
            nicknameCooldowns.upsert {
                it[nicknameCooldowns.guild] = guild
                it[nicknameCooldowns.user] = user
                it[nicknameCooldowns.timestamp] = until
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Single
class NicknameCooldownsDB : Table("nickname_cooldowns") {
    val guild = long("guild")
    val user = long("user")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(guild, user)
}

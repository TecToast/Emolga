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

@OptIn(ExperimentalTime::class)
@Single
class NicknameCooldownsRepository(val db: R2dbcDatabase) {
    suspend fun getCooldown(guild: Long, user: Long) = suspendTransaction(db) {
        NicknameCooldownsTable.select(NicknameCooldownsTable.timestamp).where {
            (NicknameCooldownsTable.guild eq guild) and (NicknameCooldownsTable.user eq user) and (NicknameCooldownsTable.timestamp greaterEq CurrentTimestamp)
        }.firstOrNull()?.get(NicknameCooldownsTable.timestamp)
    }

    suspend fun setCooldown(guild: Long, user: Long, until: Instant) {
        suspendTransaction(db) {
            NicknameCooldownsTable.upsert {
                it[NicknameCooldownsTable.guild] = guild
                it[NicknameCooldownsTable.user] = user
                it[NicknameCooldownsTable.timestamp] = until
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
object NicknameCooldownsTable : Table("nickname_cooldowns") {
    val guild = long("guild")
    val user = long("user")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(guild, user)
}

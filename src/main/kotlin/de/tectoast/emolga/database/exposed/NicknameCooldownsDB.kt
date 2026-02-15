package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.upsert
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object NicknameCooldownsDB : Table("nickname_cooldowns") {
    val GUILD = long("guild")
    val USER = long("user")
    val TIMESTAMP = timestamp("timestamp")

    override val primaryKey = PrimaryKey(GUILD, USER)

    suspend fun getCooldown(guild: Long, user: Long) = dbTransaction {
        select(TIMESTAMP).where { (GUILD eq guild) and (USER eq user) and (TIMESTAMP greaterEq CurrentTimestamp) }
            .firstOrNull()?.get(TIMESTAMP)
    }

    suspend fun setCooldown(guild: Long, user: Long, until: Instant) = dbTransaction {
        upsert {
            it[GUILD] = guild
            it[USER] = user
            it[TIMESTAMP] = until
        }
    }
}

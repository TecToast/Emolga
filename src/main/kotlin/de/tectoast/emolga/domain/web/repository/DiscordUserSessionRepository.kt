package de.tectoast.emolga.domain.web.repository

import de.tectoast.emolga.di.CleanupTask
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant


@Single
class DiscordUserSessionRepository(private val db: R2dbcDatabase, private val clock: Clock) : CleanupTask {

    override suspend fun cleanup(now: Instant) {
        suspendTransaction(db) {
            DiscordUserSessionTable.deleteWhere { DiscordUserSessionTable.expires lessEq now }
        }
    }

    suspend fun delete(id: String) {
        suspendTransaction(db) {
            DiscordUserSessionTable.deleteWhere { DiscordUserSessionTable.id eq id }
        }
    }

    suspend fun get(id: String): String = suspendTransaction(db, DiscordUserSessionTable) {
        select(content).where { this.id eq id }
            .map { it[DiscordUserSessionTable.content] }
            .first()
    }

    suspend fun set(id: String, value: String) {
        suspendTransaction(db, DiscordUserSessionTable) {
            insertIgnore {
                it[this.id] = id
                it[this.content] = value
                it[this.expires] = clock.now().plus(28.days)
            }
        }
    }
}

object DiscordUserSessionTable : Table("discord_user_session") {
    val id = text("id")
    val content = text("content")
    val expires = timestamp("expires")

    override val primaryKey = PrimaryKey(id)
}
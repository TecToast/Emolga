package de.tectoast.emolga.domain.web.repository

import de.tectoast.emolga.di.CleanupTask
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Single
class LoginCodeRepository(private val db: R2dbcDatabase, private val clock: Clock) : CleanupTask {
    suspend fun createLoginCode(userId: Long, guildId: Long): String {
        val code = Uuid.random()
        suspendTransaction(db) {
            LoginCodesTable.insert {
                it[LoginCodesTable.code] = code
                it[LoginCodesTable.userId] = userId
                it[LoginCodesTable.guildId] = guildId
                it[LoginCodesTable.timestamp] = clock.now()
            }
        }
        return code.toString()
    }

    suspend fun getUserAndGuildByCode(code: String): Pair<Long, Long>? {
        val uuid = Uuid.parseHexDashOrNull(code) ?: return null
        return suspendTransaction(db, LoginCodesTable) {
            select(userId, guildId).where {
                (LoginCodesTable.code eq uuid) and (LoginCodesTable.timestamp greaterEq clock.now().minus(10.minutes))
            }
                .singleOrNull()?.let { it[userId] to it[guildId] }
        }
    }

    suspend fun removeLoginCode(code: String) {
        val uuid = Uuid.parseHexDashOrNull(code) ?: return
        suspendTransaction(db, LoginCodesTable) {
            deleteWhere { LoginCodesTable.code eq uuid }
        }
    }

    override suspend fun cleanup(now: Instant) {
        suspendTransaction(db, LoginCodesTable) {
            deleteWhere { timestamp less now.minus(10.minutes) }
        }
    }
}

object LoginCodesTable : Table("login_codes") {
    val code = uuid("code")
    val userId = long("user_id")
    val guildId = long("guild_id")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(code)
}
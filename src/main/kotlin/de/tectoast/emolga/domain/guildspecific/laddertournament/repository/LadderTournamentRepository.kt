package de.tectoast.emolga.domain.guildspecific.laddertournament.repository

import de.tectoast.emolga.domain.guildspecific.laddertournament.model.LadderTournamentConfig
import de.tectoast.emolga.domain.guildspecific.laddertournament.model.LadderTournamentUserData
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Instant


@Single
class LadderTournamentRepository(private val db: R2dbcDatabase) {
    suspend fun getConfigByGuild(guild: Long) = suspendTransaction(db) {
        LadderTournamentConfigTable.select(LadderTournamentConfigTable.data)
            .where { LadderTournamentConfigTable.guild eq guild }.firstOrNull()?.get(LadderTournamentConfigTable.data)
    }

    suspend fun getVerifiedUsersByGuild(guild: Long) = suspendTransaction(db) {
        LadderTournamentUserTable.selectAll()
            .where { (LadderTournamentUserTable.guild eq guild) and (LadderTournamentUserTable.verified eq true) }
            .associate {
                it[LadderTournamentUserTable.userId] to LadderTournamentUserData(
                    it[LadderTournamentUserTable.sdName],
                    it[LadderTournamentUserTable.formats],
                    it[LadderTournamentUserTable.verified]
                )
            }
    }

    suspend fun isSignedUp(guild: Long, userId: Long) = suspendTransaction(db) {
        LadderTournamentUserTable.selectAll()
            .where { (LadderTournamentUserTable.guild eq guild) and (LadderTournamentUserTable.userId eq userId) and (LadderTournamentUserTable.verified eq true) }
            .count() > 0
    }

    suspend fun signupUnverified(guild: Long, userId: Long, sdName: String, formats: List<String>) =
        suspendTransaction(db) {
            LadderTournamentUserTable.deleteWhere { (LadderTournamentUserTable.guild eq guild) and (LadderTournamentUserTable.userId eq userId) }
            LadderTournamentUserTable.insert {
                it[LadderTournamentUserTable.guild] = guild
                it[LadderTournamentUserTable.userId] = userId
                it[LadderTournamentUserTable.sdName] = sdName
                it[LadderTournamentUserTable.formats] = formats
                it[LadderTournamentUserTable.verified] = false
            }
        }

    suspend fun verify(guild: Long, userId: Long) = suspendTransaction(db) {
        LadderTournamentUserTable.updateReturning(
            returning = listOf(LadderTournamentUserTable.formats),
            where = { (LadderTournamentUserTable.guild eq guild) and (LadderTournamentUserTable.userId eq userId) }
        ) {
            it[LadderTournamentUserTable.verified] = true
        }.map { it[LadderTournamentUserTable.formats] }.firstOrNull()
    }

    suspend fun getValidConfigs(now: Instant) = suspendTransaction(db, LadderTournamentConfigTable) {
        select(guild, data)/*.where { lastExecution greaterEq now }*/.map { it[guild] to it[data] }.toList()
    }
}

object LadderTournamentConfigTable : Table("ladder_tournament_config") {
    val guild = long("league")
    val lastExecution = timestamp("lastExecution")
    val data = jsonb<LadderTournamentConfig>("data")
}

object LadderTournamentUserTable : Table("ladder_tournament_users") {
    val guild = long("guild")
    val userId = long("user")
    val sdName = text("sd_name")
    val formats = array<String>("formats")
    val verified = bool("verified")

    override val primaryKey = PrimaryKey(guild, userId)
}
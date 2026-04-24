package de.tectoast.emolga.database.league

import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.config.LeagueConfig
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class LeagueQueryService(val db: R2dbcDatabase, val configRepository: LeagueConfigRepository) {
    suspend fun getByGuildUser(guild: Long, user: Long) = suspendTransaction(db) {
        LeagueCoreTable.innerJoin(LeagueUserTable, { leagueName }, { leagueName })
            .select(LeagueCoreTable.leagueName, LeagueCoreTable.configOverride, LeagueUserTable.userIndex)
            .where { (LeagueCoreTable.guild eq guild) and (LeagueUserTable.userId eq user) and (LeagueUserTable.substitute eq false) }
            .firstOrNull()?.let {
                val leagueConfig = it[LeagueCoreTable.configOverride]
                LeagueQueryResult(it[LeagueCoreTable.leagueName], configRepository.fromOverride(guild, leagueConfig), it[LeagueUserTable.userIndex])
            }
    }
}
context(iData: InteractionData)
suspend fun LeagueQueryService.byCommand() = getByGuildUser(iData.gid, iData.user)

data class LeagueQueryResult(val leagueName: String, val config: LeagueConfig, val idx: Int)

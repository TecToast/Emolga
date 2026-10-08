package de.tectoast.emolga.domain.game.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

@Single
class LeagueUsedReplayChannelRepository(private val db: R2dbcDatabase) {
    suspend fun setUsedReplayChannel(leagueName: String, replayChannel: Long) = suspendTransaction(
        db,
        LeagueUsedReplayTable
    ) {
        upsert {
            it[this.leagueName] = leagueName
            it[this.replayChannel] = replayChannel
        }
    }

    suspend fun getUsedReplayChannels(leagueName: String): Long? = suspendTransaction(db, LeagueUsedReplayTable) {
        selectAll().where { this.leagueName eq leagueName }.map { it[LeagueUsedReplayTable.replayChannel] }
            .firstOrNull()
    }
}

object LeagueUsedReplayTable : Table("league_used_replay") {
    val leagueName = text("league_name").referencesLeagueName()
    val replayChannel = long("replay_channel")

    override val primaryKey = PrimaryKey(leagueName)
}

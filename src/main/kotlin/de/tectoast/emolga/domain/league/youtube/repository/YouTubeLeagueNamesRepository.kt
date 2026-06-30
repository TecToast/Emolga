package de.tectoast.emolga.domain.league.youtube.repository

import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.koin.core.annotation.Single


@Single
class YouTubeLeagueNamesRepository(private val db: R2dbcDatabase) {
    suspend fun getPossibleGuilds(title: String) = suspendTransaction(db, YouTubeLeagueNamesTable) {
        select(guild).where { this.leagueName like "%$title%" }.map { it[guild] }.toSet()
    }
}

object YouTubeLeagueNamesTable : Table("youtube_league_names") {
    val guild = long("guild")
    val leagueName = text("league_name")

    override val primaryKey = PrimaryKey(guild, leagueName)
}
package de.tectoast.emolga.domain.league.archive.repository

import de.tectoast.emolga.domain.league.archive.model.LeagueArchiveData
import de.tectoast.emolga.utils.jsonb
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class LeagueArchiveRepository(@Named("stats") private val db: R2dbcDatabase) {
    suspend fun save(leagueName: String, data: LeagueArchiveData) = suspendTransaction(db) {
        LeagueArchiveTable.insertIgnore {
            it[LeagueArchiveTable.leagueName] = leagueName
            it[LeagueArchiveTable.data] = data
        }
    }
}


object LeagueArchiveTable : Table("st_league_archive") {
    val leagueName = text("league_name")
    val data = jsonb<LeagueArchiveData>("data")

    override val primaryKey = PrimaryKey(leagueName)
}
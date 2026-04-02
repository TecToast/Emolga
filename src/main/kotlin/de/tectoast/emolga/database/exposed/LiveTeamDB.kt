package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface LiveTeamRepository {
    suspend fun getByCode(code: Uuid): String?
    suspend fun generateForLeague(league: String): Uuid
}

@OptIn(ExperimentalUuidApi::class)
class PostgresLiveTeamRepository(val db: R2dbcDatabase, val liveTeam: LiveTeamDB) : LiveTeamRepository {
    override suspend fun getByCode(code: Uuid): String? = suspendTransaction(db) {
        liveTeam.select(liveTeam.league).where { liveTeam.code eq code }.firstOrNull()?.get(liveTeam.league)
    }

    override suspend fun generateForLeague(league: String): Uuid {
        val code = Uuid.random()
        suspendTransaction(db) {
            liveTeam.insert {
                it[liveTeam.code] = code
                it[liveTeam.league] = league
            }
        }
        return code
    }
}

@OptIn(ExperimentalUuidApi::class)
@Single
class LiveTeamDB : Table("liveteam") {
    val code = uuid("code")
    val league = varchar("league", 100)

    override val primaryKey = PrimaryKey(code)
}

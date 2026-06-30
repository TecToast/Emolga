package de.tectoast.emolga.domain.league.liveteam.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
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
@Single
class LiveTeamRepository(private val db: R2dbcDatabase) {
    suspend fun getByCode(code: Uuid): String? = suspendTransaction(db) {
        LiveTeamTable.select(LiveTeamTable.league).where { LiveTeamTable.code eq code }.firstOrNull()
            ?.get(LiveTeamTable.league)
    }

    suspend fun generateForLeague(league: String): Uuid {
        val code = Uuid.random()
        suspendTransaction(db) {
            LiveTeamTable.insert {
                it[LiveTeamTable.code] = code
                it[LiveTeamTable.league] = league
            }
        }
        return code
    }
}

@OptIn(ExperimentalUuidApi::class)
object LiveTeamTable : Table("liveteam") {
    val code = uuid("code")
    val league = text("league").referencesLeagueName()

    override val primaryKey = PrimaryKey(code)
}

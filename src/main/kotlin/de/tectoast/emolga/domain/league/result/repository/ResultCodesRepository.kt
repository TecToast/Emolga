package de.tectoast.emolga.domain.league.result.repository

import de.tectoast.emolga.di.CleanupTask
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreTable
import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.result.model.ResultCodeEntry
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
@Single
class ResultCodesRepository(private val db: R2dbcDatabase, val clock: Clock) : CleanupTask {
    suspend fun getEntryByCode(resultid: String): ResultCodeEntry? = suspendTransaction(db) {
        val uuid = Uuid.parseHexDashOrNull(resultid) ?: return@suspendTransaction null
        ResultCodesTable.innerJoin(LeagueCoreTable, { leagueName }, { leagueName })
            .select(ResultCodesTable.columns + LeagueCoreTable.guild).where { ResultCodesTable.code eq uuid }
            .singleOrNull()?.let {
                ResultCodeEntry(
                    code = it[ResultCodesTable.code],
                    leagueName = it[ResultCodesTable.leagueName],
                    guild = it[LeagueCoreTable.guild],
                    week = it[ResultCodesTable.week],
                    p1 = it[ResultCodesTable.p1],
                    p2 = it[ResultCodesTable.p2]
                )
            }
    }


    suspend fun add(leaguename: String, week: Int, p1: Int, p2: Int): Uuid {
        val code: Uuid = Uuid.random()
        suspendTransaction(db) {
            ResultCodesTable.insert {
                it[ResultCodesTable.code] = code
                it[ResultCodesTable.leagueName] = leaguename
                it[ResultCodesTable.week] = week
                it[ResultCodesTable.p1] = p1
                it[ResultCodesTable.p2] = p2
                it[ResultCodesTable.timestamp] = clock.now()
            }
        }
        return code
    }

    suspend fun delete(code: Uuid) {
        suspendTransaction(db) {
            ResultCodesTable.deleteWhere { ResultCodesTable.code eq code }
        }
    }

    override suspend fun cleanup(now: Instant) {
        suspendTransaction(db) {
            ResultCodesTable.deleteWhere { ResultCodesTable.timestamp lessEq now.minus(1.days) }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
object ResultCodesTable : Table("resultcodes") {
    val code = uuid("code")
    val leagueName = text("leaguename").referencesLeagueName()
    val week = integer("week")
    val p1 = integer("p1")
    val p2 = integer("p2")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(code)
}






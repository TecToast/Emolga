package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.exposed.LeagueEventTable.gameday
import de.tectoast.emolga.database.exposed.LeagueEventTable.uindices
import de.tectoast.emolga.database.league.LeagueScheduleTable
import de.tectoast.emolga.utils.FullGameData
import de.tectoast.emolga.utils.ReplayData
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert

object ReplayDataTable : Table("replaydata") {
    val id = integer("id").autoIncrement()
    val scheduleId = integer("schedule_id").references(LeagueScheduleTable.id)
    val matchNum = integer("matchnum")
    val data = jsonb<ReplayData>("data")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, scheduleId, matchNum)
    }
}

class ReplayDataStoreRepository(val db: R2dbcDatabase) {
    suspend fun set(leagueName: String, data: FullGameData) {
        suspendTransaction(db) {
            val id = FullGameDataTable.upsert(
                FullGameDataTable.league,
                FullGameDataTable.gameday,
                FullGameDataTable.battleindex
            ) {
                it[league] = leagueName
                it[gameday] = data.week
                it[battleindex] = data.battleIndex
                it[uindices] = data.uindices
            }[FullGameDataTable.id]
            for ((matchNum, replayData) in data.games.withIndex()) {
                ReplayDataTable.upsert(ReplayDataTable.scheduleId, ReplayDataTable.matchNum) {
                    it[ReplayDataTable.scheduleId] = id
                    it[ReplayDataTable.matchNum] = matchNum
                    it[ReplayDataTable.data] = replayData
                }
            }
        }
    }

    suspend fun get(
        leagueName: String, gameday: Int, battleIndex: Int
    ): FullGameData? = getByQuery {
        (FullGameDataTable.league eq leagueName) and (FullGameDataTable.gameday eq gameday) and (FullGameDataTable.battleindex eq battleIndex)
    }

    suspend fun getAll(leagueName: String): List<FullGameData> = suspendTransaction(db) {
        val rows = (FullGameDataTable leftJoin ReplayDataTable)
            .selectAll()
            .where { FullGameDataTable.league eq leagueName }
            .orderBy(
                FullGameDataTable.id to SortOrder.ASC,
                ReplayDataTable.matchNum to SortOrder.ASC
            )
            .toList()

        rows.groupBy { it[FullGameDataTable.id] }.map { (_, groupRows) ->
            val firstRow = groupRows.first()
            val games = groupRows.mapNotNull { it.getOrNull(ReplayDataTable.data) }

            FullGameData(
                uindices = firstRow[FullGameDataTable.uindices],
                week = firstRow[FullGameDataTable.gameday],
                battleIndex = firstRow[FullGameDataTable.battleindex],
                games = games
            )
        }
    }

    suspend fun getByGameday(
        leagueName: String, gameday: Int
    ): Map<Int, FullGameData> {
        return suspendTransaction(db) {
            FullGameDataTable.select(FullGameDataTable.data, FullGameDataTable.battleindex).where {
                (FullGameDataTable.league eq leagueName) and (FullGameDataTable.gameday eq gameday)
            }.toMap { it[FullGameDataTable.battleindex] to it[FullGameDataTable.data] }
        }
    }

    suspend fun getByIdx(
        leagueName: String, gameday: Int, idx: Int
    ): FullGameData? {
        return suspendTransaction(db) {
            FullGameDataTable.select(FullGameDataTable.data).where {
                (FullGameDataTable.league eq leagueName) and (FullGameDataTable.gameday eq gameday) and (intLiteral(idx) eq anyFrom(
                    FullGameDataTable.uindices
                ))
            }.map { it[FullGameDataTable.data] }.singleOrNull()
        }
    }

    private suspend fun getByQuery(predicate: () -> Op<Boolean>) = suspendTransaction(db) {
        val generalData = FullGameDataTable.selectAll().where(predicate).firstOrNull() ?: return@suspendTransaction null
        val id = generalData[FullGameDataTable.id]
        val replayData = ReplayDataTable.selectAll().where {
            (ReplayDataTable.scheduleId eq id)
        }.orderBy(ReplayDataTable.matchNum).map { it[ReplayDataTable.data] }.toList()
        FullGameData(
            generalData[FullGameDataTable.uindices],
            generalData[FullGameDataTable.gameday],
            generalData[FullGameDataTable.battleindex],
            replayData
        )
    }
}
package de.tectoast.emolga.domain.league.gamedata.repository

import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.domain.league.gamedata.model.GameData
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleTable
import de.tectoast.emolga.utils.groupByMapping
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.referencesCascade
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.batchUpsert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class GameDataRepository(private val db: R2dbcDatabase) {
    suspend fun storeFullGameData(leagueName: String, data: FullGameData) {
        suspendTransaction(db) {
            val scheduleId = getScheduleIdForGame(leagueName, data.week, data.battleIndex) ?: return@suspendTransaction

            GameDataTable.batchUpsert(
                data.games.withIndex(), shouldReturnGeneratedValues = false
            ) { (matchNum, replayData) ->
                this[GameDataTable.scheduleId] = scheduleId
                this[GameDataTable.matchNum] = matchNum
                this[GameDataTable.data] = replayData
            }
        }
    }


    suspend fun getFullGameData(leagueName: String, week: Int, battleIndex: Int): FullGameData? =
        suspendTransaction(db) {
            val (scheduleId, uindices) = getScheduleIdAndIndicesForGame(leagueName, week, battleIndex)
                ?: return@suspendTransaction null
            val games = GameDataTable.select(GameDataTable.data).where {
                GameDataTable.scheduleId eq scheduleId
            }.orderBy(GameDataTable.matchNum).map { it[GameDataTable.data] }.toList()
            FullGameData(uindices, week, battleIndex, games)
        }

    suspend fun getFullGameDataForWeekIfAllPresent(leagueName: String, week: Int): List<FullGameData>? =
        suspendTransaction(db) {
            val grouped = LeagueScheduleTable.leftJoin(GameDataTable, { this.id }, { this.scheduleId })
                .select(
                    LeagueScheduleTable.battleIndex,
                    LeagueScheduleTable.p1,
                    LeagueScheduleTable.p2,
                    GameDataTable.data
                )
                .where { (LeagueScheduleTable.leagueName eq leagueName) and (LeagueScheduleTable.week eq week) }
                .orderBy(LeagueScheduleTable.battleIndex to SortOrder.ASC, GameDataTable.matchNum to SortOrder.ASC)
                .groupByMapping({
                    it[LeagueScheduleTable.battleIndex] to listOf(
                        it[LeagueScheduleTable.p1],
                        it[LeagueScheduleTable.p2]
                    )
                }) {
                    it.getOrNull(GameDataTable.data)
                }
            if (grouped.values.flatten().any { it == null }) return@suspendTransaction null
            grouped.map { (battleData, singleGameDatas) ->
                FullGameData(uindices = battleData.second, week, battleData.first, singleGameDatas.filterNotNull())
            }
        }

    suspend fun getAllGameDataUntil(leagueName: String, maxWeek: Int?) = suspendTransaction(db) {
        GameDataTable.innerJoin(LeagueScheduleTable).select(GameDataTable.data, LeagueScheduleTable.week)
            .where { LeagueScheduleTable.leagueName eq leagueName }.apply {
                maxWeek?.let { andWhere { LeagueScheduleTable.week lessEq it } }
            }.map { it[GameDataTable.data] to it[LeagueScheduleTable.week] }.toList()
    }

    private suspend fun getScheduleIdForGame(leagueName: String, week: Int, battleIndex: Int): Int? =
        LeagueScheduleTable.select(LeagueScheduleTable.id).where {
            (LeagueScheduleTable.leagueName eq leagueName) and (LeagueScheduleTable.week eq week) and (LeagueScheduleTable.battleIndex eq battleIndex)
        }.firstOrNull()?.get(LeagueScheduleTable.id)

    private suspend fun getScheduleIdAndIndicesForGame(
        leagueName: String,
        week: Int,
        battleIndex: Int
    ): Pair<Int, List<Int>>? =
        with(LeagueScheduleTable) {
            select(id, p1, p2).where {
                (this.leagueName eq leagueName) and (this.week eq week) and (this.battleIndex eq battleIndex)
            }.map { it[id] to listOf(it[p1], it[p2]) }.firstOrNull()
        }

}

object GameDataTable : Table("game_data") {
    val scheduleId = integer("schedule_id").referencesCascade(LeagueScheduleTable.id)
    val matchNum = integer("matchnum")
    val data = jsonb<GameData>("data")

    override val primaryKey = PrimaryKey(scheduleId, matchNum)
}

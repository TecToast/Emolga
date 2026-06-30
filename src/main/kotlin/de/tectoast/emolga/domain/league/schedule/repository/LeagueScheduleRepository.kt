package de.tectoast.emolga.domain.league.schedule.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.schedule.model.ScheduleData
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.koin.core.annotation.Single


@Single
class LeagueScheduleRepository(private val db: R2dbcDatabase) {
    suspend fun getScheduleData(leagueName: String, idx1: Int, idx2: Int) =
        suspendTransaction(db, LeagueScheduleTable) {
            select(
                this.week, this.battleIndex, this.p1, this.p2
            ).where {
                (this.leagueName eq leagueName) and (((this.p1 eq idx1) and (this.p2 eq idx2)) or ((this.p1 eq idx2) and (this.p2 eq idx1)))
            }.orderBy(this.week, SortOrder.DESC).firstOrNull()?.let {
                ScheduleData(
                    week = it[this.week],
                    battleIndex = it[this.battleIndex],
                    indices = listOf(it[this.p1], it[this.p2]),
                )
            }
        }

    suspend fun getMatchUp(leagueName: String, week: Int, battleIndex: Int) =
        suspendTransaction(db, LeagueScheduleTable) {
            select(p1, p2).where {
                (this.leagueName eq leagueName) and (this.week eq week) and (this.battleIndex eq battleIndex)
            }.firstOrNull()?.let { listOf(it[p1], it[p2]) }
        }

    suspend fun getMatchUpsForWeek(leagueName: String, week: Int) = suspendTransaction(db, LeagueScheduleTable) {
        select(p1, p2, battleIndex).where { (this.leagueName eq leagueName) and (this.week eq week) }
            .orderBy(battleIndex)
            .map { ScheduleData(week, it[battleIndex], listOf(it[p1], it[p2])) }
            .toList()
    }

    suspend fun getBattleIndex(leagueName: String, week: Int, idx: Int) = suspendTransaction(db, LeagueScheduleTable) {
        select(battleIndex).where { (this.leagueName eq leagueName) and (this.week eq week) and ((this.p1 eq idx) or (this.p2 eq idx)) }
            .firstOrNull()?.get(battleIndex)
    }

    suspend fun setSchedule(leagueName: String, schedule: Map<Int, List<List<Int>>>) =
        suspendTransaction(db, LeagueScheduleTable) {
            deleteWhere { this.leagueName eq leagueName }
            batchInsert(schedule.flatMap { (week, battles) ->
                battles.mapIndexed { battleIndex, indices ->
                    ScheduleData(
                        week,
                        battleIndex,
                        indices
                    )
                }
            }) { (week, battleIndex, indices) ->
                this[LeagueScheduleTable.leagueName] = leagueName
                this[LeagueScheduleTable.week] = week
                this[LeagueScheduleTable.battleIndex] = battleIndex
                this[LeagueScheduleTable.p1] = indices[0]
                this[LeagueScheduleTable.p2] = indices[1]
            }
        }
}

object LeagueScheduleTable : Table("league_schedule") {
    val id = integer("id").autoIncrement()
    val leagueName = text("league_name").referencesLeagueName()
    val week = integer("week")
    val battleIndex = integer("battle_index")

    val p1 = integer("p1")
    val p2 = integer("p2")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, leagueName, week, battleIndex)
        index(false, leagueName, p1, p2)
    }
}
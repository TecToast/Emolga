package de.tectoast.emolga.domain.league.gamedata.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.gamedata.model.LeagueEvent
import de.tectoast.emolga.domain.league.gamedata.model.LeagueEventSpecificData
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class LeagueEventRepository(private val db: R2dbcDatabase) {
    suspend fun hasPlayedGame(leaguename: String, week: Int, idx: Int) = suspendTransaction(db) {
        LeagueEventTable.selectAll()
            .where {
                (LeagueEventTable.leaguename eq leaguename) and
                        (LeagueEventTable.week eq week) and
                        (intParam(idx) eq anyFrom(LeagueEventTable.uindices))
            }.count() > 0
    }

    suspend fun addEvents(events: Iterable<LeagueEvent>) = suspendTransaction(db, LeagueEventTable) {
        LeagueEventTable.batchInsert(events, ignore = true, shouldReturnGeneratedValues = false) { event ->
            this[leaguename] = event.leagueName
            this[week] = event.week
            this[matchNum] = event.matchNum
            this[timestamp] = event.timestamp
            this[uindices] = event.uindices
            this[specificData] = event.specificData
        }
    }

    suspend fun getRelevantEvents(leaguename: String, uindices: List<Int>) = suspendTransaction(db) {
        LeagueEventTable.selectAll()
            .where {
                (LeagueEventTable.leaguename eq leaguename) and
                        (LeagueEventTable.uindices containedBy uindices)
            }.map { it.toLeagueEvent() }.toList()
    }

    suspend fun getAllEvents(leaguename: String) = suspendTransaction(db) {
        LeagueEventTable.selectAll()
            .where { LeagueEventTable.leaguename eq leaguename }
            .map { it.toLeagueEvent() }.toList()
    }

    private fun ResultRow.toLeagueEvent() = LeagueEvent(
        leagueName = this[LeagueEventTable.leaguename],
        week = this[LeagueEventTable.week],
        matchNum = this[LeagueEventTable.matchNum],
        timestamp = this[LeagueEventTable.timestamp],
        uindices = this[LeagueEventTable.uindices],
        specificData = this[LeagueEventTable.specificData]
    )
}

object LeagueEventTable : Table("league_events") {

    val leaguename = text("leaguename").referencesLeagueName()
    val week = integer("week")
    val timestamp = timestamp("timestamp")
    val uindices = array<Int>("uindices")
    val matchNum = integer("matchnum")

    val specificData = jsonb<LeagueEventSpecificData>("specific_data")

    override val primaryKey = PrimaryKey(leaguename, week, uindices, matchNum)
}

class ContainedByOp(
    private val column: Expression<*>,
    private val value: Expression<*>
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        column.toQueryBuilder(queryBuilder)
        queryBuilder.append(" <@ ")
        value.toQueryBuilder(queryBuilder)
    }
}

infix fun <T> ExpressionWithColumnType<T>.containedBy(value: T): Op<Boolean> =
    ContainedByOp(this, QueryParameter(value, this.columnType))
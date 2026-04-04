package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.UserTableData
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Instant

object LeagueEventTable : Table("league_events") {

    val leaguename = varchar("leaguename", 100)
    val gameday = integer("week")
    val timestamp = timestamp("timestamp")
    val uindices = array<Int>("uindices")
    val matchNum = integer("matchnum")

    val specificData = jsonb<LeagueEventSpecificData>("specific_data")

    override val primaryKey = PrimaryKey(leaguename, gameday, uindices, matchNum)
}

@Single
class LeagueEventRepository(val db: R2dbcDatabase) {
    suspend fun hasPlayedGame(leaguename: String, gameday: Int, idx: Int) = suspendTransaction(db) {
        LeagueEventTable.selectAll()
            .where {
                (LeagueEventTable.leaguename eq leaguename) and
                        (LeagueEventTable.gameday eq gameday) and
                        (intParam(idx) eq anyFrom(LeagueEventTable.uindices))
            }.count() > 0
    }

    suspend fun addEvent(event: LeagueEvent) = suspendTransaction(db) {
        LeagueEventTable.insert {
            it[leaguename] = event.leagueName
            it[gameday] = event.gameday
            it[matchNum] = event.matchNum
            it[timestamp] = event.timestamp
            it[uindices] = event.uindices
            it[specificData] = event.specificData
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
        gameday = this[LeagueEventTable.gameday],
        matchNum = this[LeagueEventTable.matchNum],
        timestamp = this[LeagueEventTable.timestamp],
        uindices = this[LeagueEventTable.uindices],
        specificData = this[LeagueEventTable.specificData]
    )
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

data class LeagueEvent(
    val leagueName: String,
    val gameday: Int,
    val matchNum: Int,
    val timestamp: Instant,
    val uindices: List<Int>,
    val specificData: LeagueEventSpecificData
)

@Serializable
sealed interface LeagueEventSpecificData {

    fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>)

    sealed class Sanction : LeagueEventSpecificData {
        abstract val reason: String
        abstract val issuer: Long
    }

    @Serializable
    @SerialName("MatchResult")
    data class MatchResult(
        val data: List<Int>
    ) : LeagueEventSpecificData {
        override fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>) {
            for ((i, idx) in event.uindices.withIndex()) {
                map[idx]!!.let {
                    val k = data[i]
                    val d = data[1 - i]
                    it.kills += k
                    it.deaths += d
                    it.points += if (k > d) 3 else 0
                    it.wins += if (k > d) 1 else 0
                    it.losses += if (k < d) 1 else 0
                }
            }
        }
    }


    @Serializable
    @SerialName("Zeroed")
    class ZeroedGame(
        override val reason: String,
        override val issuer: Long,
    ) : Sanction() {
        override fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>) {
            event.uindices.forEach { idx ->
                map[idx]?.let { data ->
                    data.deaths += 6
                }
            }
        }
    }

    @Serializable
    @SerialName("PointPenalty")
    data class PointPenalty(
        val amount: Int,
        override val reason: String,
        override val issuer: Long,
    ) : Sanction() {
        override fun manipulate(event: LeagueEvent, map: MutableMap<Int, UserTableData>) {
            event.uindices.forEach { idx ->
                map[idx]?.let { data ->
                    data.points -= amount
                }
            }
        }
    }
}
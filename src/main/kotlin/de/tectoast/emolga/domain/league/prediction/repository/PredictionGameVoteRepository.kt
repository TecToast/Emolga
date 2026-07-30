package de.tectoast.emolga.domain.league.prediction.repository

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreTable
import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.prediction.model.PredictionGameVoteData
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single

@Single
class PredictionGameVoteRepository(
    private val db: R2dbcDatabase,
) {
    suspend fun updateCorrectBattles(league: String, week: Int, battle: Int, winnerIdx: Int) {
        suspendTransaction(db) {
            PredictionGameVotesTable.update({
                (PredictionGameVotesTable.leagueName eq league) and
                        (PredictionGameVotesTable.week eq week) and
                        (PredictionGameVotesTable.battle eq battle)
            }) {
                it[PredictionGameVotesTable.correct] = PredictionGameVotesTable.idx eq winnerIdx
            }
        }
    }

    suspend fun addVote(user: Long, league: String, week: Int, battle: Int, idx: Int) {
        suspendTransaction(db) {
            PredictionGameVotesTable.upsert {
                it[PredictionGameVotesTable.userId] = user
                it[PredictionGameVotesTable.leagueName] = league
                it[PredictionGameVotesTable.week] = week
                it[PredictionGameVotesTable.battle] = battle
                it[PredictionGameVotesTable.idx] = idx
            }
        }
    }

    suspend fun getAllPredictionGameVotes(leagueName: String) = suspendTransaction(db, PredictionGameVotesTable) {
        PredictionGameVotesTable.selectAll().where { PredictionGameVotesTable.leagueName eq leagueName }
            .map { it.rowToData() }
            .toList()
    }

    private fun ResultRow.rowToData(): PredictionGameVoteData = with(PredictionGameVotesTable) {
        PredictionGameVoteData(get(leagueName), get(userId), get(week), get(battle), get(idx), get(correct))
    }

    suspend fun getAllPredictionGameVotesForWeek(guild: Long, week: Int) = suspendTransaction(db) {
        PredictionGameVotesTable.innerJoin(LeagueCoreTable, { this.leagueName }, { this.leagueName })
            .select(PredictionGameVotesTable.columns)
            .where { (LeagueCoreTable.guild eq guild) and (PredictionGameVotesTable.week eq week) }
            .orderBy(
                LeagueCoreTable.num to SortOrder.ASC,
                PredictionGameVotesTable.userId to SortOrder.ASC,
                PredictionGameVotesTable.battle to SortOrder.ASC
            )
            .map { it.rowToData() }
            .toList()
    }

    suspend fun getVoteCountBeforeWeek(guild: Long, week: Int) = suspendTransaction(db) {
        PredictionGameVotesTable.innerJoin(LeagueCoreTable, { this.leagueName }, { this.leagueName })
            .selectAll()
            .where { (LeagueCoreTable.guild eq guild) and (PredictionGameVotesTable.week less week) }
            .count()
    }
}

object PredictionGameVotesTable : Table("predictiongamevotes") {
    val leagueName = text("leaguename").referencesLeagueName()
    val userId = long("userid")
    val week = integer("week")
    val battle = integer("battle")
    val idx = integer("idx")
    val correct = bool("correct").nullable().default(null)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(leagueName, week, battle, userId)
}

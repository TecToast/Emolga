package de.tectoast.emolga.domain.league.prediction.repository

import de.tectoast.emolga.domain.league.core.repository.referencesLeagueName
import de.tectoast.emolga.domain.league.prediction.model.PredictionGameVoteData
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert
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
            .map { PredictionGameVoteData(it[userId], it[week], it[battle], it[idx], it[correct]) }
            .toList()
    }
}

object PredictionGameVotesTable : Table("predictiongamevotes") {
    val leagueName = text("leaguename").referencesLeagueName()
    val userId = long("userid")
    val week = integer("week")
    val battle = integer("battle")
    val idx = integer("idx")
    val correct = bool("correct").nullable().default(null)

    override val primaryKey = PrimaryKey(leagueName, userId, week, battle)
}

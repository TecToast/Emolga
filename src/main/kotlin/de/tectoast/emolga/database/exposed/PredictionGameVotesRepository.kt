package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.features.league.K18n_PredictionGameCommand
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.CalcResult
import de.tectoast.emolga.utils.json.mdb
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

data class BasicUserPredictionScore(
    val userId: Long,
    val correctCount: Int
)

data class AdvancedUserPredictionScore(
    val userId: Long,
    val correctVotes: Int,
    val totalVotes: Int
)

data class AboveBelowUserPredictionScore(
    val rank: Int,
    val userId: Long,
    val correctVotes: Int,
    val totalVotes: Int,
    val isTarget: Boolean
)

data class MissingVotesData(
    val data: Map<String, List<List<Long>>>,
    val amount: Int
)

data class OwnVotesData(
    val u1: Long,
    val u2: Long,
    val selected: Long,
    val correct: Boolean?
)

data class CorrectTotalData(
    val correct: Int,
    val total: Int
)

private const val ABOVE_BELOW_COUNT = 3

// TODO maybe create a service
@Single
class PredictionGameVoteRepository(val db: R2dbcDatabase) {
    suspend fun updateCorrectBattles(league: String, gameday: Int, battle: Int, winningIndex: Int) {
        suspendTransaction(db) {
            PredictionGameVotesTable.update({
                (PredictionGameVotesTable.leaguename eq league) and
                        (PredictionGameVotesTable.week eq gameday) and
                        (PredictionGameVotesTable.battle eq battle)
            }) {
                it[PredictionGameVotesTable.correct] = PredictionGameVotesTable.idx eq winningIndex
            }
        }
    }

    suspend fun addVote(user: Long, league: String, gameday: Int, battle: Int, idx: Int) {
        suspendTransaction(db) {
            PredictionGameVotesTable.upsert {
                it[PredictionGameVotesTable.userid] = user
                it[PredictionGameVotesTable.leaguename] = league
                it[PredictionGameVotesTable.week] = gameday
                it[PredictionGameVotesTable.battle] = battle
                it[PredictionGameVotesTable.idx] = idx
            }
        }
    }

    suspend fun getCurrentVoteState(league: String, gameday: Int, battle: Int): Map<Int, Long> =
        suspendTransaction(db) {
            val count = PredictionGameVotesTable.userid.count()
            PredictionGameVotesTable.select(PredictionGameVotesTable.idx, count)
                .where { (PredictionGameVotesTable.leaguename eq league) and (PredictionGameVotesTable.week eq gameday) and (PredictionGameVotesTable.battle eq battle) }
                .groupBy(PredictionGameVotesTable.idx).toMap { it[PredictionGameVotesTable.idx] to it[count] }
        }

    suspend fun getFullResultsSummary(league: String) = suspendTransaction(db) {
        with(PredictionGameVotesTable) {
            val correctCount = correct.count()
            select(userid, correctCount)
                .where { (this.leaguename eq leaguename) and (correct eq true) }
                .groupBy(userid)
                .orderBy(correctCount, SortOrder.DESC)
                .map { BasicUserPredictionScore(it[userid], it[correctCount].toInt()) }
                .toList()
        }
    }

    suspend fun getTopNOfGuild(guild: Long, n: Int) = suspendTransaction(db) {
        with(PredictionGameVotesTable) {
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()
            val leaguePredicate = leaguePredicate(guild)
            select(userid, correctVotes, totalVotes)
                .where { leaguePredicate }
                .groupBy(userid)
                .orderBy(correctVotes, SortOrder.DESC)
                .limit(n)
                .map { AdvancedUserPredictionScore(it[userid], it[correctVotes] ?: 0, it[totalVotes].toInt()) }
                .toList()
        }
    }

    suspend fun getStatsWithAboveAndBelow(guild: Long, userId: Long) = suspendTransaction(db) {
        with(PredictionGameVotesTable) {
            val leaguePredicate = leaguePredicate(guild)
            val correctVotesWithoutAlias = getCorrectExpression()
            val correctVotes = correctVotesWithoutAlias.alias("correct_votes")
            val totalVotes = getTotalExpression().alias("total_votes")
            val rankExpression = Rank().over().orderBy(correctVotesWithoutAlias, SortOrder.DESC).alias("rank")
            val leaderboardAlias = select(userid, correctVotes, totalVotes, rankExpression)
                .where { leaguePredicate }
                .groupBy(userid)
                .alias("leaderboard_subquery")

            val userCol = leaderboardAlias[userid]
            val rankCol = leaderboardAlias[rankExpression]
            val correctCol = leaderboardAlias[correctVotes]
            val totalCol = leaderboardAlias[totalVotes]

            val targetRank = leaderboardAlias.select(rankCol).where { userCol eq userId }.singleOrNull()?.get(rankCol)
                ?: return@suspendTransaction null
            leaderboardAlias
                .select(rankCol, userCol, correctCol, totalCol)
                .where { rankCol.between(targetRank - ABOVE_BELOW_COUNT, targetRank + ABOVE_BELOW_COUNT) }
                .orderBy(rankCol to SortOrder.ASC, (userCol eq userId) to SortOrder.DESC)
                .toList()
                .groupBy { it[rankCol] }
                .flatMap { (_, rows) ->
                    rows.take(ABOVE_BELOW_COUNT + 1).map { row ->
                        val rank = row[rankCol]
                        val user = row[userCol]
                        val correct = row[correctCol] ?: 0
                        val total = row[totalCol]
                        val isTarget = (user == userId)
                        AboveBelowUserPredictionScore(
                            rank.toInt(), user, correct, total.toInt(), isTarget
                        )
                    }
                }
        }
    }


    suspend fun getUserStatsPerLeague(guild: Long, userId: Long) = suspendTransaction(db) {
        with(PredictionGameVotesTable) {
            val leaguesInOrder = allLeagues(guild)
            val leaguePredicate =
                leaguename.inList(leaguesInOrder.map { it.leaguename })
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()

            val dataMap = select(leaguename, correctVotes, totalVotes)
                .where { (leaguePredicate) and (userid eq userId) }
                .groupBy(leaguename)
                .toMap { row ->
                    val leagueName = row[leaguename]
                    val correct = row[correctVotes] ?: 0
                    val total = row[totalVotes]
                    leagueName to CorrectTotalData(correct, total.toInt())
                }
            leaguesInOrder.associate {
                it.displayName to (dataMap[it.leaguename] ?: CorrectTotalData(0, 0))
            }
        }
    }

    suspend fun getMissingVotesForGameday(guild: Long, gameday: Int, user: Long) =
        suspendTransaction(db) {
            with(PredictionGameVotesTable) {
                val allLeagues = allLeagues(guild)
                val leaguePredicate = leaguePredicate(allLeagues.map { it.leaguename })
                val presentData = select(leaguename, battle)
                    .where { (leaguePredicate) and (this.week eq gameday) and (userid eq user) }
                    .toList()
                    .groupBy { it[leaguename] }
                var amount = 0
                val result = allLeagues.mapNotNull { league ->
                    val battleorder = league.battleorder[gameday] ?: return@mapNotNull null
                    val present = presentData[league.leaguename]
                    val missingBattles = battleorder.indices.toList() - present?.map { it[battle] }.orEmpty().toSet()
                    amount += missingBattles.size
                    if (missingBattles.isEmpty()) return@mapNotNull null
                    league.displayName to battleorder.filterIndexed { index, _ -> missingBattles.contains(index) }.map {
                        listOf(league[it[0]], league[it[1]])
                    }
                }.toMap()
                MissingVotesData(result, amount)
            }
        }

    suspend fun getOwnVotesForLeagueAndGameday(
        guild: Long,
        leagueName: String,
        gameday: Int,
        userId: Long,
    ): CalcResult<List<OwnVotesData>> {
        val league =
            mdb.leagueByDisplayName(guild, leagueName) ?: return CalcResult.Error(
                K18n_PredictionGameCommand.OwnVotesLeagueNotFound(
                    leagueName
                )
            )
        val games =
            league.battleorder[gameday] ?: return CalcResult.Error(
                K18n_PredictionGameCommand.OwnVotesGamedayNotFound(
                    leagueName,
                    gameday
                )
            )
        return suspendTransaction(db) {
            CalcResult.Success(with(PredictionGameVotesTable) {
                select(battle, idx, correct)
                    .where { (leaguename eq league.leaguename) and (this.week eq gameday) and (userid eq userId) }
                    .orderBy(battle to SortOrder.ASC).map { row ->
                        val battle = row[battle]
                        val correct = row[correct]
                        val idx = row[idx]
                        OwnVotesData(league[games[battle][0]], league[games[battle][1]], league[idx], correct)
                    }.toList()
            })
        }
    }


    private fun PredictionGameVotesTable.getTotalExpression(): Count = Count(correct)

    private fun PredictionGameVotesTable.getCorrectExpression(): Sum<Int> = Sum(
        Case()
            .When(correct eq true, intLiteral(1))
            .Else(intLiteral(0)),
        IntegerColumnType()
    )

    private suspend fun PredictionGameVotesTable.leaguePredicate(
        gid: Long
    ) = leaguePredicate(allLeagueNames(gid))

    private fun PredictionGameVotesTable.leaguePredicate(
        names: List<String>
    ) = leaguename.inList(names)


    private suspend fun allLeagues(gid: Long): List<League> = mdb.leaguesByGuild(gid).sortedBy { it.num }
    private suspend fun allLeagueNames(gid: Long): List<String> = allLeagues(gid).map { it.leaguename }
}

package de.tectoast.emolga.domain.league.prediction.service

import de.tectoast.emolga.domain.league.core.model.ScalarLeagueData
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.prediction.model.*
import de.tectoast.emolga.domain.league.prediction.repository.PredictionGameVotesTable
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.features.league.K18n_PredictionGameCommand
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


private const val ABOVE_BELOW_COUNT = 3

@Single
class PredictionGameAnalyseService(
    private val db: R2dbcDatabase,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val leagueMemberRepo: LeagueMemberRepository
) {
    suspend fun getCurrentVoteState(league: String, week: Int, battle: Int): Map<Int, Long> =
        suspendTransaction(db) {
            val count = PredictionGameVotesTable.userId.count()
            PredictionGameVotesTable.select(PredictionGameVotesTable.idx, count)
                .where { (PredictionGameVotesTable.leagueName eq league) and (PredictionGameVotesTable.week eq week) and (PredictionGameVotesTable.battle eq battle) }
                .groupBy(PredictionGameVotesTable.idx)
                .associate { it[PredictionGameVotesTable.idx] to it[count] }
        }

    suspend fun getFullResultsSummary(leagueName: String) = suspendTransaction(db) {
        with(PredictionGameVotesTable) {
            val correctCount = correct.count()
            select(userId, correctCount)
                .where { (this.leagueName eq leagueName) and (correct eq true) }
                .groupBy(userId)
                .orderBy(correctCount, SortOrder.DESC)
                .map { BasicUserPredictionScore(it[userId], it[correctCount].toInt()) }
                .toList()
        }
    }

    suspend fun getTopNOfGuild(guild: Long, n: Int) = suspendTransaction(db) {
        with(PredictionGameVotesTable) {
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()
            val leaguePredicate = leaguePredicate(guild)
            select(userId, correctVotes, totalVotes)
                .where { leaguePredicate }
                .groupBy(userId)
                .orderBy(correctVotes, SortOrder.DESC)
                .limit(n)
                .map { AdvancedUserPredictionScore(it[userId], it[correctVotes] ?: 0, it[totalVotes].toInt()) }
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
            val leaderboardAlias = select(this.userId, correctVotes, totalVotes, rankExpression)
                .where { leaguePredicate }
                .groupBy(this.userId)
                .alias("leaderboard_subquery")

            val userCol = leaderboardAlias[this.userId]
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
                leagueName.inList(leaguesInOrder.map { it.leagueName })
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()

            val dataMap = select(leagueName, correctVotes, totalVotes)
                .where { (leaguePredicate) and (this.userId eq userId) }
                .groupBy(leagueName)
                .associate {
                    val leagueName1 = it[leagueName]
                    val correct1 = it[correctVotes] ?: 0
                    val total1 = it[totalVotes]
                    leagueName1 to CorrectTotalData(correct1, total1.toInt())
                }
            leaguesInOrder.associate {
                it.displayName to (dataMap[it.leagueName] ?: CorrectTotalData(0, 0))
            }
        }
    }

    suspend fun getMissingVotesForWeek(guild: Long, week: Int, user: Long) =
        suspendTransaction(db) {
            with(PredictionGameVotesTable) {
                val allLeagues = allLeagues(guild)
                val leaguePredicate = leaguePredicate(allLeagues.map { it.leagueName })
                val presentData = select(leagueName, battle)
                    .where { (leaguePredicate) and (this.week eq week) and (userId eq user) }
                    .toList()
                    .groupBy { it[leagueName] }
                var amount = 0
                val result = allLeagues.mapNotNull { league ->
                    val battleorder = leagueScheduleRepo.getMatchUpsForWeek(league.leagueName, week)
                    val present = presentData[league.leagueName]
                    val missingBattles = battleorder.indices.toList() - present?.map { it[battle] }.orEmpty().toSet()
                    amount += missingBattles.size
                    if (missingBattles.isEmpty()) return@mapNotNull null
                    val primaryIds = leagueMemberRepo.getPrimaryIds(league.leagueName)
                    league.displayName to battleorder.filterIndexed { index, _ -> missingBattles.contains(index) }.map {
                        listOf(primaryIds[it.indices[0]].orEmpty(), primaryIds[it.indices[1]].orEmpty())
                    }
                }.toMap()
                MissingVotesData(result, amount)
            }
        }

    suspend fun getOwnVotesForLeagueAndWeek(
        guild: Long,
        displayName: String,
        week: Int,
        userId: Long,
    ): CalcResult<List<OwnVotesData>> {
        val leagueName = leagueCoreRepo.getLeagueNameByDisplayName(guild, displayName) ?: return CalcResult.Error(
            K18n_PredictionGameCommand.OwnVotesLeagueNotFound(displayName)
        )
        val games = leagueScheduleRepo.getMatchUpsForWeek(leagueName, week).takeIf { it.isNotEmpty() }
            ?: return K18n_PredictionGameCommand.OwnVotesWeekNotFound(leagueName, week).error()
        val primaryIds = leagueMemberRepo.getPrimaryIds(leagueName)
        return suspendTransaction(db, PredictionGameVotesTable) {
            select(battle, idx, correct)
                .where { (this.leagueName eq leagueName) and (this.week eq week) and (this.userId eq userId) }
                .orderBy(battle to SortOrder.ASC).map { row ->
                    val battle = row[battle]
                    val correct = row[correct]
                    val idx = row[idx]
                    OwnVotesData(
                        primaryIds[games[battle].indices[0]].orEmpty(),
                        primaryIds[games[battle].indices[1]].orEmpty(),
                        primaryIds[idx].orEmpty(),
                        correct
                    )
                }.toList()
        }.success()
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
    ) = leagueName.inList(names)


    private suspend fun allLeagues(gid: Long): List<ScalarLeagueData> = leagueCoreRepo.getAllScalarLeagueData(gid)
    private suspend fun allLeagueNames(gid: Long): List<String> =
        leagueCoreRepo.getAllScalarLeagueData(gid).map { it.leagueName }
}

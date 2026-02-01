package de.tectoast.emolga.utils

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.TipGameVotesDB
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.UpsertSqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.statements.UpsertSqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.jdbc.select

object TipGameAnalyseService {
    suspend fun getFullTipGameResultsSummary(leaguename: String) = dbTransaction {
        with(TipGameVotesDB) {
            val correctCount = CORRECT.count()
            select(USERID, correctCount)
                .where { (LEAGUENAME eq leaguename) and (CORRECT eq true) }
                .groupBy(USERID)
                .orderBy(correctCount, SortOrder.DESC)
                .mapIndexed { index, it -> "${index + 1}. <@${it[USERID]}>: ${it[correctCount]}" }
                .joinToString("\n", prefix = "```", postfix = "```")
        }
    }

    suspend fun getTop10OfGuild(gid: Long) = dbTransaction {
        with(TipGameVotesDB) {
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()
            val leaguePredicate = leaguePredicate(gid)
            select(USERID, correctVotes, totalVotes)
                .where { leaguePredicate }
                .groupBy(USERID)
                .orderBy(correctVotes, SortOrder.DESC)
                .limit(10)
                .mapIndexed { index, it ->
                    val userId = it[USERID]
                    val correct = it[correctVotes] ?: 0
                    val total = it[totalVotes]
                    val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                    "#%d: <@%d> (%d/%d, %.2f%%)".format(index + 1, userId, correct, total, percentage)
                }
                .joinToString("\n").ifEmpty { "_Es wurden noch keine Tipps abgegeben._" }
        }
    }


    private const val ABOVE_BELOW = 3
    suspend fun getTipGameStatsWithAboveAndBelow(gid: Long, userId: Long) = dbTransaction {
        with(TipGameVotesDB) {
            val leaguePredicate = leaguePredicate(gid)
            val correctVotes = getCorrectExpression().alias("correct_votes")
            val totalVotes = getTotalExpression().alias("total_votes")
            val rankExpression = Rank().over().orderBy(correctVotes, SortOrder.DESC).alias("rank")
            val leaderboardAlias = select(USERID, correctVotes, totalVotes, rankExpression)
                .where { leaguePredicate }
                .groupBy(USERID)
                .alias("leaderboard_subquery")

            val userCol = leaderboardAlias[USERID]
            val rankCol = leaderboardAlias[rankExpression]
            val correctCol = leaderboardAlias[correctVotes]
            val totalCol = leaderboardAlias[totalVotes]

            val targetRank = leaderboardAlias.select(rankCol).where { userCol eq userId }.singleOrNull()?.get(rankCol)
            if (targetRank == null) {
                return@dbTransaction null
            }
            leaderboardAlias
                .select(rankCol, userCol, correctCol, totalCol)
                .where { rankCol.between(targetRank - ABOVE_BELOW, targetRank + ABOVE_BELOW) }
                .orderBy(rankCol, SortOrder.ASC)
                .joinToString("\n") { row ->
                    val rank = row[rankCol]
                    val user = row[userCol]
                    val correct = row[correctCol] ?: 0
                    val total = row[totalCol]
                    val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                    val isTarget = (user == userId)
                    "#$rank: <@${user}> ($correct/$total, ${"%.2f".format(percentage)}%) ${if (isTarget) "<-- DU" else ""}"
                }
        }
    }

    suspend fun getUserTipGameStatsPerLeague(gid: Long, userId: Long) = dbTransaction {
        with(TipGameVotesDB) {
            val leaguesInOrder = de.tectoast.emolga.utils.json.db.leaguesByGuild(gid).sortedBy { it.num }
            val leaguePredicate =
                LEAGUENAME.inList(leaguesInOrder.map { it.leaguename })
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()

            val dataMap = select(LEAGUENAME, correctVotes, totalVotes)
                .where { (leaguePredicate) and (USERID eq userId) }
                .groupBy(LEAGUENAME)
                .associate { row ->
                    val leagueName = row[LEAGUENAME]
                    val correct = row[correctVotes] ?: 0
                    val total = row[totalVotes]
                    val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                    leagueName to "%d/%d (%.2f%%)".format(correct, total, percentage)
                }
            leaguesInOrder.joinToString("\n") { league ->
                val stats = dataMap[league.leaguename] ?: "0/0 (0.00%)"
                val displayName = league.config.tipgame?.withName ?: league.leaguename
                "**$displayName**: $stats"
            }
        }
    }

    private fun TipGameVotesDB.getTotalExpression(): Count = Count(CORRECT)

    private fun TipGameVotesDB.getCorrectExpression(): Sum<Int> = Sum(
        Case()
            .When(CORRECT eq true, intLiteral(1))
            .Else(intLiteral(0)),
        IntegerColumnType()
    )

    private suspend fun TipGameVotesDB.leaguePredicate(
        gid: Long
    ) = LEAGUENAME.inList(de.tectoast.emolga.utils.json.db.leaguesByGuild(gid).map { it.leaguename })
}
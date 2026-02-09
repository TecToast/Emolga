package de.tectoast.emolga.utils

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.TipGameVotesDB
import de.tectoast.emolga.features.draft.K18n_TipGameCommand
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.db
import de.tectoast.k18n.generated.K18nLanguage
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

    suspend fun getTop10OfGuild(gid: Long, language: K18nLanguage) = dbTransaction {
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
                .joinToString("\n").ifEmpty { K18n_TipGameCommand.Top10NoTips.translateTo(language) }
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
                .orderBy(rankCol to SortOrder.ASC, (userCol eq userId) to SortOrder.DESC)
                .groupBy { it[rankCol] }
                .flatMap { (_, rows) ->
                    rows.take(4).map { row ->
                        val rank = row[rankCol]
                        val user = row[userCol]
                        val correct = row[correctCol] ?: 0
                        val total = row[totalCol]
                        val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                        val isTarget = (user == userId)
                        "#$rank: <@${user}> ($correct/$total, ${"%.2f".format(percentage)}%) ${if (isTarget) "<-- DU" else ""}"
                    }
                }
                .joinToString("\n")
        }
    }

    suspend fun getUserTipGameStatsPerLeague(gid: Long, userId: Long) = dbTransaction {
        with(TipGameVotesDB) {
            val leaguesInOrder = allLeagues(gid)
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
                "**${league.displayName}**: $stats"
            }
        }
    }

    suspend fun getMissingVotesForGameday(gid: Long, gameday: Int, user: Long, language: K18nLanguage) = dbTransaction {
        with(TipGameVotesDB) {
            val allLeagues = allLeagues(gid)
            val leaguePredicate = leaguePredicate(allLeagues.map { it.leaguename })
            val presentData = select(LEAGUENAME, BATTLE)
                .where { (leaguePredicate) and (GAMEDAY eq gameday) and (USERID eq user) }
                .groupBy { it[LEAGUENAME] }
            var amount = 0
            val str = allLeagues.mapNotNull { league ->
                val battleorder = league.battleorder[gameday] ?: return@mapNotNull null
                val present = presentData[league.leaguename]
                val missingBattles = battleorder.indices.toList() - present?.map { it[BATTLE] }.orEmpty().toSet()
                amount += missingBattles.size
                if (missingBattles.isEmpty()) return@mapNotNull null
                "## ${league.displayName}\n" +
                        battleorder.filterIndexed { index, _ -> missingBattles.contains(index) }.joinToString("\n") {
                            "<@${league[it[0]]}> vs <@${league[it[1]]}>"
                        }
            }.joinToString("\n")
            if (str.isEmpty()) K18n_TipGameCommand.CheckMissingAllTipsGiven(gameday)
            else {
                if (str.length > 1900) K18n_TipGameCommand.CheckMissingTooLong(amount)
                else
                    K18n_TipGameCommand.CheckMissingSuccess(gameday, str)
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
    ) = leaguePredicate(allLeagueNames(gid))

    private fun TipGameVotesDB.leaguePredicate(
        names: List<String>
    ) = LEAGUENAME.inList(names)


    private suspend fun allLeagues(gid: Long): List<League> = db.leaguesByGuild(gid).sortedBy { it.num }
    private suspend fun allLeagueNames(gid: Long): List<String> = allLeagues(gid).map { it.leaguename }
}
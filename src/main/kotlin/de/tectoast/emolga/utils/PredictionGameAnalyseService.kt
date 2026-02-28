package de.tectoast.emolga.utils

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.PredictionGameVotesDB
import de.tectoast.emolga.database.exposed.joinToString
import de.tectoast.emolga.database.exposed.toMap
import de.tectoast.emolga.features.league.K18n_PredictionGameCommand
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.generic.K18n_You
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.select

object PredictionGameAnalyseService {
    suspend fun getFullResultsSummary(leaguename: String) = dbTransaction {
        with(PredictionGameVotesDB) {
            val correctCount = CORRECT.count()
            select(USERID, correctCount)
                .where { (LEAGUENAME eq leaguename) and (CORRECT eq true) }
                .groupBy(USERID)
                .orderBy(correctCount, SortOrder.DESC)
                .mapIndexed { index, it -> "${index + 1}. <@${it[USERID]}>: ${it[correctCount]}" }
                .joinToString("\n", prefix = "```", postfix = "```")
        }
    }

    suspend fun getTopNOfGuild(gid: Long, n: Int) = dbTransaction {
        val content = with(PredictionGameVotesDB) {
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()
            val leaguePredicate = leaguePredicate(gid)
            select(USERID, correctVotes, totalVotes)
                .where { leaguePredicate }
                .groupBy(USERID)
                .orderBy(correctVotes, SortOrder.DESC)
                .limit(n)
                .mapIndexed { index, it ->
                    val userId = it[USERID]
                    val correct = it[correctVotes] ?: 0
                    val total = it[totalVotes]
                    val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                    "#%d: <@%d> (%d/%d, %.2f%%)".format(index + 1, userId, correct, total, percentage)
                }
                .joinToString("\n")
        }
        if (content.isEmpty()) K18n_PredictionGameCommand.TopNNoPredictions
        else K18n_PredictionGameCommand.TopNSuccess(n, content)
    }


    private const val ABOVE_BELOW = 3
    suspend fun getStatsWithAboveAndBelow(gid: Long, userId: Long, language: K18nLanguage) = dbTransaction {
        with(PredictionGameVotesDB) {
            val leaguePredicate = leaguePredicate(gid)
            val correctVotesWithoutAlias = getCorrectExpression()
            val correctVotes = correctVotesWithoutAlias.alias("correct_votes")
            val totalVotes = getTotalExpression().alias("total_votes")
            val rankExpression = Rank().over().orderBy(correctVotesWithoutAlias, SortOrder.DESC).alias("rank")
            val leaderboardAlias = select(USERID, correctVotes, totalVotes, rankExpression)
                .where { leaguePredicate }
                .groupBy(USERID)
                .alias("leaderboard_subquery")

            val userCol = leaderboardAlias[USERID]
            val rankCol = leaderboardAlias[rankExpression]
            val correctCol = leaderboardAlias[correctVotes]
            val totalCol = leaderboardAlias[totalVotes]

            val targetRank = leaderboardAlias.select(rankCol).where { userCol eq userId }.singleOrNull()?.get(rankCol)
                ?: return@dbTransaction null
            leaderboardAlias
                .select(rankCol, userCol, correctCol, totalCol)
                .where { rankCol.between(targetRank - ABOVE_BELOW, targetRank + ABOVE_BELOW) }
                .orderBy(rankCol to SortOrder.ASC, (userCol eq userId) to SortOrder.DESC)
                .toList()
                .groupBy { it[rankCol] }
                .flatMap { (_, rows) ->
                    rows.take(4).map { row ->
                        val rank = row[rankCol]
                        val user = row[userCol]
                        val correct = row[correctCol] ?: 0
                        val total = row[totalCol]
                        val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                        val isTarget = (user == userId)
                        "#$rank: <@${user}> ($correct/$total, ${"%.2f".format(percentage)}%) ${
                            if (isTarget) "<-- ${
                                K18n_You.translateTo(
                                    language
                                ).uppercase()
                            }" else ""
                        }"
                    }
                }
                .joinToString("\n")
        }
    }

    suspend fun getUserStatsPerLeague(gid: Long, userId: Long) = dbTransaction {
        with(PredictionGameVotesDB) {
            val leaguesInOrder = allLeagues(gid)
            val leaguePredicate =
                LEAGUENAME.inList(leaguesInOrder.map { it.leaguename })
            val correctVotes = getCorrectExpression()
            val totalVotes = getTotalExpression()

            val dataMap = select(LEAGUENAME, correctVotes, totalVotes)
                .where { (leaguePredicate) and (USERID eq userId) }
                .groupBy(LEAGUENAME)
                .toMap { row ->
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
        with(PredictionGameVotesDB) {
            val allLeagues = allLeagues(gid)
            val leaguePredicate = leaguePredicate(allLeagues.map { it.leaguename })
            val presentData = select(LEAGUENAME, BATTLE)
                .where { (leaguePredicate) and (GAMEDAY eq gameday) and (USERID eq user) }
                .toList()
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
            if (str.isEmpty()) K18n_PredictionGameCommand.CheckMissingAllPredictionsGiven(gameday)
            else {
                if (str.length > 1900) K18n_PredictionGameCommand.CheckMissingTooLong(amount)
                else
                    K18n_PredictionGameCommand.CheckMissingSuccess(gameday, str)
            }
        }
    }

    suspend fun getOwnVotesForLeagueAndGameday(
        gid: Long,
        leagueName: String,
        gameday: Int,
        userId: Long,
        language: K18nLanguage
    ): String {
        val league =
            mdb.leagueByDisplayName(gid, leagueName) ?: return K18n_PredictionGameCommand.OwnVotesLeagueNotFound(
                leagueName
            )
                .translateTo(language)
        val games =
            league.battleorder[gameday] ?: return K18n_PredictionGameCommand.OwnVotesGamedayNotFound(
                leagueName,
                gameday
            )
            .translateTo(language)
        return dbTransaction {
            with(PredictionGameVotesDB) {
                select(BATTLE, IDX, CORRECT)
                    .where { (LEAGUENAME eq league.leaguename) and (GAMEDAY eq gameday) and (USERID eq userId) }
                    .orderBy(BATTLE to SortOrder.ASC).joinToString("\n") { row ->
                        val battle = row[BATTLE]
                        val correct = row[CORRECT]
                        val idx = row[IDX]
                        "<@${league[games[battle][0]]}> vs <@${league[games[battle][1]]}>: <@${league[idx]}>".notNullAppend(
                            correct
                        ) { if (it) " ✅" else " ❌" }
                    }.ifEmpty { K18n_PredictionGameCommand.OwnVotesNoVotes(leagueName, gameday).translateTo(language) }
            }
        }
    }


    private fun PredictionGameVotesDB.getTotalExpression(): Count = Count(CORRECT)

    private fun PredictionGameVotesDB.getCorrectExpression(): Sum<Int> = Sum(
        Case()
            .When(CORRECT eq true, intLiteral(1))
            .Else(intLiteral(0)),
        IntegerColumnType()
    )

    private suspend fun PredictionGameVotesDB.leaguePredicate(
        gid: Long
    ) = leaguePredicate(allLeagueNames(gid))

    private fun PredictionGameVotesDB.leaguePredicate(
        names: List<String>
    ) = LEAGUENAME.inList(names)


    private suspend fun allLeagues(gid: Long): List<League> = mdb.leaguesByGuild(gid).sortedBy { it.num }
    private suspend fun allLeagueNames(gid: Long): List<String> = allLeagues(gid).map { it.leaguename }
}

fun <T, R> Flow<T>.mapIndexed(transform: suspend (Int, T) -> R) = flow {
    var index = 0
    collect { emit(transform(index++, it)) }
}

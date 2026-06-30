package de.tectoast.emolga.domain.league.prediction.service

import de.tectoast.emolga.features.league.K18n_PredictionGameCommand
import de.tectoast.emolga.utils.*
import de.tectoast.generic.K18n_You
import org.koin.core.annotation.Single

@Single
class PredictionGameAnalyseTextService(
    private val votes: PredictionGameAnalyseService
) {
    suspend fun getFullResultsSummary(league: String): String {
        return votes.getFullResultsSummary(league)
            .mapIndexed { index, it -> "${index + 1}. <@${it.userId}>: ${it.correctCount}" }
            .joinToString("\n", prefix = "```", postfix = "```")
    }

    suspend fun getTopNOfGuild(guild: Long, n: Int): K18nMessageOrError {
        val topNOfGuild = votes.getTopNOfGuild(guild, n)
        if (topNOfGuild.isEmpty()) return K18n_PredictionGameCommand.TopNNoPredictions.error()
        val content = topNOfGuild
            .mapIndexed { index, it ->
                val userId = it.userId
                val correct = it.correctVotes
                val total = it.totalVotes
                val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                "#%d: <@%d> (%d/%d, %.2f%%)".format(index + 1, userId, correct, total, percentage)
            }
            .joinToString("\n")
        return K18n_PredictionGameCommand.TopNSuccess(n, content).success()
    }

    private suspend fun getStatsWithAboveAndBelow(guild: Long, userId: Long): K18nMessageOrError {
        val stats =
            votes.getStatsWithAboveAndBelow(guild, userId)
                ?: return K18n_PredictionGameCommand.SelfNoPredictions.error()
        return b {
            stats.joinToString("\n") {
                val rank = it.rank
                val user = it.userId
                val correct = it.correctVotes
                val total = it.totalVotes
                val percentage = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                val isTarget = (user == userId)
                "#$rank: <@${user}> ($correct/$total, ${"%.2f".format(percentage)}%) ${
                    if (isTarget) "<-- ${
                        K18n_You().uppercase()
                    }" else ""
                }"
            }
        }.success()
    }

    private suspend fun getUserStatsPerLeague(guild: Long, userId: Long): CalcResult<String> {
        val stats = votes.getUserStatsPerLeague(guild, userId)
        if (stats.isEmpty()) return K18n_PredictionGameCommand.SelfNoPredictions.error()
        return stats.entries.joinToString("\n") { (league, data) ->
            val correct = data.correct
            val total = data.total
            val percentage = if (data.total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
            "**%s**: %d/%d (%.2f%%)".format(league, correct, total, percentage)
        }.success()
    }

    suspend fun selfData(guild: Long, userId: Long): K18nMessageOrError {
        val aboveAndBelow = getStatsWithAboveAndBelow(guild, userId).getOrReturn { return it }
        val resultsPerLeague = getUserStatsPerLeague(
            guild, userId
        ).getOrReturn { return it }

        return b {
            K18n_PredictionGameCommand.SelfSuccess(aboveAndBelow(), resultsPerLeague)()
        }.success()

    }

    suspend fun getMissingVotesForWeek(guild: Long, week: Int, user: Long): K18nMessageOrError {
        val missingVotes = votes.getMissingVotesForWeek(guild, week, user)
        val amount = missingVotes.amount
        if (amount == 0) return K18n_PredictionGameCommand.CheckMissingAllPredictionsGiven(week).error()
        val str = missingVotes.data.entries.joinToString("\n") { (leagueDisplayName, matchUps) ->
            "## ${leagueDisplayName}\n" + matchUps.joinToString("\n") { l ->
                buildString {
                    append(l[0].joinToTeammates())
                    append(" vs ")
                    append(l[1].joinToTeammates())
                }
            }
        }
        return if (str.length > 1900) K18n_PredictionGameCommand.CheckMissingTooLong(amount).error()
        else K18n_PredictionGameCommand.CheckMissingSuccess(week, str).success()
    }

    suspend fun getOwnVotesForLeagueAndWeek(
        guild: Long,
        leagueName: String,
        week: Int,
        userId: Long
    ): K18nMessageOrError {
        val ownVotes = votes.getOwnVotesForLeagueAndWeek(guild, leagueName, week, userId).getOrReturn { return it }
        return b {
            ownVotes.joinToString("\n") { data ->
                buildString {
                    append(data.u1.joinToTeammates())
                    append(" vs ")
                    append(data.u2.joinToTeammates())
                    append(": ")
                    append(data.selected.joinToTeammates())
                    when (data.correct) {
                        true -> append(" ✅")
                        false -> append(" ❌")
                        null -> {}
                    }
                }
            }
        }.success()
    }


}
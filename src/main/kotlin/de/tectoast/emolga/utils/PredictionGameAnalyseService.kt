package de.tectoast.emolga.utils

import de.tectoast.emolga.database.exposed.PredictionGameVoteRepository
import de.tectoast.emolga.features.league.K18n_PredictionGameCommand
import de.tectoast.emolga.utils.json.*
import de.tectoast.generic.K18n_You
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Single


@Single
class PredictionGameAnalyseService(
    private val votes: PredictionGameVoteRepository
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

    suspend fun getStatsWithAboveAndBelow(guild: Long, userId: Long): K18nMessageOrError {
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

    suspend fun getUserStatsPerLeague(guild: Long, userId: Long): CalcResult<String> {
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

    suspend fun getMissingVotesForGameday(guild: Long, gameday: Int, user: Long): K18nMessageOrError {
        val missingVotes = votes.getMissingVotesForGameday(guild, gameday, user)
        val amount = missingVotes.amount
        if (amount == 0) return K18n_PredictionGameCommand.CheckMissingAllPredictionsGiven(gameday).error()
        val str = missingVotes.data.entries.joinToString("\n") {
            "## ${it.key}\n" + it.value.joinToString("\n") { l ->
                "<@${l[0]}> vs <@${l[1]}>"
            }
        }
        return if (str.length > 1900) K18n_PredictionGameCommand.CheckMissingTooLong(amount).error()
        else K18n_PredictionGameCommand.CheckMissingSuccess(gameday, str).success()
    }

    suspend fun getOwnVotesForLeagueAndGameday(
        guild: Long,
        leagueName: String,
        gameday: Int,
        userId: Long
    ): K18nMessageOrError {
        val data = votes.getOwnVotesForLeagueAndGameday(guild, leagueName, gameday, userId).getOrReturn { return it }
        return b {
            data.joinToString("\n") {
                "<@${it.u1}> vs <@${it.u2}>: <@${it.selected}>".notNullAppend(
                    it.correct
                ) { c -> if (c) " ✅" else " ❌" }
            }
        }.success()
    }


}

fun <T, R> Flow<T>.mapIndexed(transform: suspend (Int, T) -> R) = flow {
    var index = 0
    collect { emit(transform(index++, it)) }
}

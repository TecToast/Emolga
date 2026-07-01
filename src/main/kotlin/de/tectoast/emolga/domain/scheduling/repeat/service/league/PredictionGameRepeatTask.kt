package de.tectoast.emolga.domain.scheduling.repeat.service.league

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.prediction.model.config.PredictionGameConfig
import de.tectoast.emolga.domain.league.prediction.service.PredictionGameLeaderboardService
import de.tectoast.emolga.domain.league.prediction.service.PredictionGameService
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import org.koin.core.annotation.Single

@Single
class PredictionGameRepeatTask(
    private val predictionGameService: PredictionGameService,
    private val predictionGameLeaderboardService: PredictionGameLeaderboardService
) : LeagueRepeatTask {
    override suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: LeagueConfig
    ) {
        val predictionConfig = config.predictionGame ?: return
        predictionConfig.setupSendTask(scheduler, leagueName)
        predictionConfig.setupLockButtonsTask(scheduler, leagueName)
        predictionConfig.setupLeaderboardTask(scheduler, leagueName)
    }

    private fun PredictionGameConfig.setupSendTask(scheduler: RepeatTaskScheduler, leagueName: String) {
        scheduler.schedule(
            RepeatTask(
                RepeatTaskType.PredictionGameSending(leagueName),
                lastSending,
                amount,
                interval,
                printTimestamps = true,
                skipFirstN = skipFirstN
            )
        ) { week ->
            predictionGameService.send(leagueName, week)
        }
    }

    private fun PredictionGameConfig.setupLockButtonsTask(scheduler: RepeatTaskScheduler, leagueName: String) {
        val lastLockButtons = lastLockButtons ?: return
        scheduler.schedule(
            RepeatTask(
                RepeatTaskType.PredictionGameLockButtons(leagueName),
                lastLockButtons,
                amount,
                interval
            )
        ) { week ->
            predictionGameService.lockButtons(leagueName, week)
        }
    }

    private fun PredictionGameConfig.setupLeaderboardTask(scheduler: RepeatTaskScheduler, leagueName: String) {
        val leaderboardConfig = leaderboardConfig ?: return
        scheduler.schedule(
            RepeatTask(
                RepeatTaskType.PredictionGameLeaderboard(leagueName),
                lastSending + interval,
                amount,
                interval
            )
        ) { _ ->
            predictionGameLeaderboardService.sendNewLeaderboard(leagueName, leaderboardConfig)
        }
    }
}



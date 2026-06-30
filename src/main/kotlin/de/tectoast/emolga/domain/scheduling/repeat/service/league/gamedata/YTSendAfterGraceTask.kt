package de.tectoast.emolga.domain.scheduling.repeat.service.league.gamedata

import de.tectoast.emolga.domain.game.model.GameDataStoreConfig
import de.tectoast.emolga.domain.game.model.YTEnableConfig
import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.youtube.service.YouTubeSendService
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import org.koin.core.annotation.Single

@Single
class YTSendAfterGraceTask(private val service: YouTubeSendService) : GameDataTask {
    override suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: GameDataStoreConfig,
        battleIndex: Int,
        leagueConfig: LeagueConfig
    ) {
        leagueConfig.youtube?.sendChannel?.let { _ ->
            val (lastExecutionBase, interval) = when {
                config.ytEnableConfig is YTEnableConfig.Custom -> config.ytEnableConfig.lastUploadStart to config.ytEnableConfig.intervalBetweenMatches
                else -> config.lastUploadStart to config.intervalBetweenMatches
            }
            scheduler.schedule(
                RepeatTask(
                    RepeatTaskType.YTSendAfterGrace(leagueName, battleIndex),
                    lastExecutionBase + interval * battleIndex + config.gracePeriodForYT,
                    config.amount,
                    config.intervalBetweenWeeks
                )
            ) { week ->
                service.send(leagueName, week, battleIndex)
            }
        }
    }
}




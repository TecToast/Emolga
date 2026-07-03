package de.tectoast.emolga.domain.scheduling.repeat.service.league.gamedata

import de.tectoast.emolga.domain.game.model.ScheduledGameRegisterConfig
import de.tectoast.emolga.domain.game.model.YTEnableConfig
import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.youtube.repository.YTVideoSendRepository
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import org.koin.core.annotation.Single

@Single
class CustomYTEnableTask(private val ytVideoSendRepo: YTVideoSendRepository) : ScheduledGameRegisterTask {
    override suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: ScheduledGameRegisterConfig,
        battleIndex: Int,
        leagueConfig: LeagueConfig
    ) {
        val ytConfig = config.ytEnableConfig
        if (ytConfig is YTEnableConfig.Custom) {
            scheduler.schedule(
                RepeatTask(
                    RepeatTaskType.YTEnable(leagueName, battleIndex),
                    ytConfig.lastUploadStart + ytConfig.intervalBetweenMatches * battleIndex,
                    config.amount,
                    config.intervalBetweenWeeks
                )
            ) { week ->
                ytVideoSendRepo.enable(leagueName, week, battleIndex)
            }
        }
    }
}

package de.tectoast.emolga.domain.scheduling.repeat.service.league.gamedata

import de.tectoast.emolga.domain.game.model.ScheduledGameRegisterConfig
import de.tectoast.emolga.domain.game.model.YTEnableConfig
import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.doc.service.RegisterInDocService
import de.tectoast.emolga.domain.league.youtube.repository.YTVideoSendRepository
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import org.koin.core.annotation.Single

@Single
class RegisterInDocTask(
    private val ytVideoSendRepo: YTVideoSendRepository,
    private val registerInDocService: RegisterInDocService
) :
    ScheduledGameRegisterTask {
    override suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: ScheduledGameRegisterConfig,
        battleIndex: Int,
        leagueConfig: LeagueConfig
    ) {
        scheduler.schedule(
            RepeatTask(
                RepeatTaskType.RegisterInDoc(leagueName, battleIndex),
                config.lastUploadStart + config.intervalBetweenMatches * battleIndex,
                config.amount,
                config.intervalBetweenWeeks
            )
        ) { week ->
            if (config.ytEnableConfig is YTEnableConfig.WithDocEntry) {
                ytVideoSendRepo.enable(leagueName, week, battleIndex)
            }
            registerInDocService.registerInDoc(leagueName, week, battleIndex)
        }
    }
}


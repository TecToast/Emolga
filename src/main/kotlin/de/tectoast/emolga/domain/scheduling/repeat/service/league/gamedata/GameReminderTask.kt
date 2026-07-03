package de.tectoast.emolga.domain.scheduling.repeat.service.league.gamedata

import de.tectoast.emolga.domain.game.model.ScheduledGameRegisterConfig
import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.gamedata.service.GameReminderService
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import org.koin.core.annotation.Single

@Single
class GameReminderTask(private val service: GameReminderService) : ScheduledGameRegisterTask {
    override suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: ScheduledGameRegisterConfig,
        battleIndex: Int,
        leagueConfig: LeagueConfig
    ) {
        scheduler.schedule(
            RepeatTask(
                RepeatTaskType.SendReminderToParticipants(leagueName, battleIndex),
                config.lastUploadStart + config.intervalBetweenMatches * battleIndex,
                config.amount,
                config.intervalBetweenWeeks
            )
        ) {
            service.sendReminder(leagueName, it, battleIndex)
        }
    }
}


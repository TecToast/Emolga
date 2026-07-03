package de.tectoast.emolga.domain.scheduling.repeat.service.league.gamedata

import de.tectoast.emolga.domain.game.model.ScheduledGameRegisterConfig
import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler

interface ScheduledGameRegisterTask {
    suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: ScheduledGameRegisterConfig,
        battleIndex: Int,
        leagueConfig: LeagueConfig
    )
}

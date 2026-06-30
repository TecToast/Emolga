package de.tectoast.emolga.domain.scheduling.repeat.service.league

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler

interface LeagueRepeatTask {
    suspend fun setup(scheduler: RepeatTaskScheduler, leagueName: String, config: LeagueConfig)
}

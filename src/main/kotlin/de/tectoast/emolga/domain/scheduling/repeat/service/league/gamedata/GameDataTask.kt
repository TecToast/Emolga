package de.tectoast.emolga.domain.scheduling.repeat.service.league.gamedata

import de.tectoast.emolga.domain.game.model.GameDataStoreConfig
import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler

interface GameDataTask {
    suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: GameDataStoreConfig,
        battleIndex: Int,
        leagueConfig: LeagueConfig
    )
}

package de.tectoast.emolga.domain.scheduling.repeat.service.league

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.domain.scheduling.repeat.service.RepeatTaskScheduler
import de.tectoast.emolga.domain.scheduling.repeat.service.league.gamedata.GameDataTask
import org.koin.core.annotation.Single

@Single
class GameDataStoreRepeatTask(
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val tasks: List<GameDataTask>,
) : LeagueRepeatTask {
    override suspend fun setup(
        scheduler: RepeatTaskScheduler,
        leagueName: String,
        config: LeagueConfig
    ) {
        val gameDataStoreConfig = config.gameDataStore ?: return
        val size = leagueScheduleRepo.getMatchUpsForWeek(leagueName, 1).size
        for (task in tasks) {
            repeat(size) { battleIndex ->
                task.setup(scheduler, leagueName, gameDataStoreConfig, battleIndex, config)
            }
        }
    }
}

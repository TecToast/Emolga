package de.tectoast.emolga.domain.scheduling.repeat.service

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.scheduling.repeat.service.league.LeagueRepeatTask
import org.koin.core.annotation.Single

@Single
class LeagueStartupService(
    private val scheduler: RepeatTaskScheduler,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val tasks: List<LeagueRepeatTask>
) : StartupTask {
    override suspend fun onStartup() {
        for (leagueName in leagueCoreRepo.getAllLeagueNames()) {
            val config = leagueConfigRepo.getConfig(leagueName)
            for (task in tasks) {
                task.setup(scheduler, leagueName, config)
            }
        }
    }

}

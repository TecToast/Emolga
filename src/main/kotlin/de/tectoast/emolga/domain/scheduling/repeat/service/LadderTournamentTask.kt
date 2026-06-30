package de.tectoast.emolga.domain.scheduling.repeat.service

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.guildspecific.laddertournament.repository.LadderTournamentRepository
import de.tectoast.emolga.domain.guildspecific.laddertournament.service.LadderTournamentService
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Single
class LadderTournamentTask(
    private val scheduler: RepeatTaskScheduler,
    private val ladderTournamentRepo: LadderTournamentRepository,
    private val service: LadderTournamentService,
    private val clock: Clock,
) : StartupTask {
    override suspend fun onStartup() {
        ladderTournamentRepo.getValidConfigs(clock.now()).forEach { (guild, config) ->
            scheduler.schedule(
                RepeatTask(
                    RepeatTaskType.Other("LadderTournament $guild"),
                    Instant.fromEpochMilliseconds(config.lastExecution),
                    config.amount,
                    config.durationInHours.hours
                )
            ) {
                service.executeForGuild(guild)
            }
        }
    }
}

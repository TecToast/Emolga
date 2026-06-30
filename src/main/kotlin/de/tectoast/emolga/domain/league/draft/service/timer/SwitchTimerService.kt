package de.tectoast.emolga.domain.league.draft.service.timer

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.draft.model.timer.TimerInfo
import de.tectoast.emolga.domain.util.service.TimeFormatService
import de.tectoast.emolga.features.league.K18n_SwitchTimer
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class SwitchTimerService(
    private val leagueConfigRepo: LeagueConfigRepository,
    private val timeFormatService: TimeFormatService
) {
    suspend fun create(
        leagueName: String, settings: List<String>, stallSeconds: Int = 0, from: Int = 0, to: Int = 24
    ): CalcResult<DraftTimerConfig.SwitchTimer> {
        val timer = DraftTimerConfig.SwitchTimer(settings.associateWith {
            val minutes =
                timeFormatService.parseDuration(it).inWholeMinutes.takeIf { n -> n >= 0 }
                    ?: return K18n_SwitchTimer.InvalidTime(it)
                        .error()
            TimerInfo(minutes.toInt()).set(from, to)
        })
        timer.stallSeconds = stallSeconds
        leagueConfigRepo.updateLeagueOverride(leagueName) {
            copy(timer = timer)
        }
        return timer.success()
    }


    suspend fun switchTo(leagueName: String, switchTo: String): CalcResult<DraftTimerConfig.SwitchTimer> {
        val override = leagueConfigRepo.updateLeagueOverride(leagueName) {
            val switchTimer = timer as? DraftTimerConfig.SwitchTimer ?: run {
                return K18n_SwitchTimer.NoSwitchTimer(leagueName).error()
            }
            if (switchTo !in switchTimer.timerInfos) {
                return K18n_SwitchTimer.InvalidTime(switchTo).error()
            }
            switchTimer.currentTimer = switchTo
            this
        }
        return (override.timer as DraftTimerConfig.SwitchTimer).success()
    }
}
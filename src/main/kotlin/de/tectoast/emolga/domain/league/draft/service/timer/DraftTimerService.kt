package de.tectoast.emolga.domain.league.draft.service.timer


import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.timer.DelayData
import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.draft.model.timer.TimerRelated
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.invoke
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.isDistantPast

private val dayTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val timeFormatSecs = DateTimeFormatter.ofPattern("HH:mm:ss")

private val SECONDS_THRESHOLD = 15.minutes

@Single
class DraftTimerService(
    private val clock: Clock, private val calcService: DraftTimerCalculationService, dispatcher: CoroutineDispatcher
) {
    private val allTimers = ConcurrentHashMap<String, Job>()
    private val allStallSecondTimers = ConcurrentHashMap<String, Job>()

    private val _expiredTimerEvents = MutableSharedFlow<String>()
    private val _expiredStallSecondEvents = MutableSharedFlow<String>()
    val expiredTimerEvents = _expiredTimerEvents.asSharedFlow()
    val expiredStallSecondEvents = _expiredStallSecondEvents.asSharedFlow()

    private val timerScope = createCoroutineScope("DraftTimerService", dispatcher)

    fun cancelTimer(leagueName: String) {
        allTimers.remove(leagueName)?.cancel()
        allStallSecondTimers.remove(leagueName)?.cancel()
    }

    fun isValidTimerExecution(cooldown: Instant): Boolean {
        return cooldown > Instant.DISTANT_PAST && cooldown <= clock.now()
    }

    fun continueTimer(ctx: DraftRunContext) {
        val timerRelated = ctx.league.draftData.timer
        if (timerRelated.cooldown.isDistantPast) return
        val delayData = DelayData(
            timerRelated.cooldown, timerRelated.regularCooldown, clock.now()
        )
        startTimer(ctx.league.leagueName, timerRelated, ctx.config.timer?.stallSeconds, delayData)
    }

    fun startRegularTimer(ctx: DraftRunContext) {
        val now = clock.now()
        val leagueName = ctx.league.leagueName
        val currentIdx = ctx.league.currentIdx
        val timerRelated = ctx.league.draftData.timer
        val timerConfig = ctx.config.timer

        val delayData = timerConfig?.let {
            calcService.calc(
                config = it,
                now = now,
                howOftenSkipped = ctx.league.draftData.punishableSkippedTurns[currentIdx]?.size ?: 0,
                usedStallSeconds = timerRelated.usedStallSeconds[currentIdx] ?: 0
            )
        } ?: return disableTimer(
            leagueName, timerRelated
        )
        startTimer(leagueName, timerRelated, timerConfig.stallSeconds, delayData, now)
    }

    private fun startTimer(
        leagueName: String,
        timerRelated: TimerRelated,
        stallSeconds: Int?,
        delayData: DelayData,
        now: Instant = clock.now()
    ) {
        val skipDelay = delayData.skipDelay
        with(timerRelated) {
            lastPick = now
            lastStallSecondUsedMid = null
            lastRegularDelay = delayData.regularDelay
            cooldown = delayData.skipTimestamp
            regularCooldown = delayData.regularTimestamp
            cancelTimer(leagueName)
            allTimers[leagueName] = launchTimerTask(skipDelay, leagueName, _expiredTimerEvents)
            stallSeconds?.takeIf { it > 0 }?.let { _ ->
                val regularDelay = delayData.regularDelay
                if (delayData.hasStallSeconds && !regularDelay.isNegative()) {
                    allStallSecondTimers[leagueName] =
                        launchTimerTask(regularDelay, leagueName, _expiredStallSecondEvents)
                }
            }
        }
    }

    private fun launchTimerTask(delay: Duration, leagueName: String, eventFlow: MutableSharedFlow<String>): Job =
        timerScope.launch {
            delay(delay)
            withContext(NonCancellable) {
                eventFlow.emit(leagueName)
            }
        }

    private fun disableTimer(leagueName: String, timerRelated: TimerRelated) {
        timerRelated.cooldown = Instant.DISTANT_PAST
        timerRelated.regularCooldown = Instant.DISTANT_PAST
        cancelTimer(leagueName)
    }

    fun getCurrentTimerMessage(timerConfig: DraftTimerConfig, timerData: TimerRelated): K18nMessage {
        return b {
            buildString {
                append(
                    K18n_League.TimeUntil(
                        formatTimeFormatBasedOnDistance(
                            timerData.regularCooldown, timerConfig.stallSeconds
                        )
                    )()
                )
                if (timerData.stallSecondsActive()) {
                    append(
                        K18n_League.TimeUntilStallSeconds(
                            formatTimeFormatBasedOnDistance(
                                timerData.cooldown, timerConfig.stallSeconds
                            )
                        )()
                    )
                }
            }

        }
    }

    private fun formatTimeFormatBasedOnDistance(cooldown: Instant, stallSeconds: Int?): String {
        val delay = cooldown - clock.now()
        val formatType = when {
            delay >= 1.days -> "f"
            stallSeconds != null -> "T"
            else -> "t"
        }
        return "<t:${cooldown.epochSeconds}:$formatType>"
    }
}

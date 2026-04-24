package de.tectoast.emolga.database.league


import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.league.config.TimerRelated
import de.tectoast.emolga.utils.*
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

private val dayTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val timeFormatSecs = DateTimeFormatter.ofPattern("HH:mm:ss")

private const val SECONDS_THRESHOLD = 15 * 60 * 1000

@Single
class DraftTimerService(val clock: Clock, dispatcher: CoroutineDispatcher) {
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

    fun isValidTimerExecution(cooldown: Long): Boolean {
        return cooldown in 0..clock.now().toEpochMilliseconds()
    }

    fun restartTimer(ctx: DraftRunContext, delayData: DelayData?) {
        restartTimer(
            ctx.league.leagueName,
            ctx.league.currentIdx,
            ctx.config.timer,
            ctx.league.draftData.punishableSkippedTurns,
            ctx.league.draftData.timer,
            delayData
        )
    }

    fun restartTimer(
        leagueName: String,
        currentIdx: Int,
        timerConfig: DraftTimer?,
        punishableSkippedTurns: Map<Int, Collection<Int>>,
        timerRelated: TimerRelated,
        delayData: DelayData?
    ) {
        val howOftenSkipped = punishableSkippedTurns[currentIdx]?.size ?: 0
        val usedStallSeconds = timerRelated.usedStallSeconds[currentIdx] ?: 0
        val now = clock.now().toEpochMilliseconds()
        val actualDelayData =
            delayData ?: timerConfig?.calc(now, howOftenSkipped, usedStallSeconds) ?: return disableTimer(
                leagueName,
                timerRelated
            )
        val skipDelay = actualDelayData.skipDelay
        with(timerRelated) {
            lastPick = now
            lastStallSecondUsedMid = 0
            lastRegularDelay = actualDelayData.regularDelay
            cooldown = actualDelayData.skipTimestamp
            regularCooldown = actualDelayData.regularTimestamp
            cancelTimer(leagueName)
            allTimers[leagueName] = launchTimerTask(skipDelay, leagueName, _expiredTimerEvents)
            timerConfig?.stallSeconds?.takeIf { it > 0 }?.let { stallSeconds ->
                val regularDelay = actualDelayData.regularDelay
                if (actualDelayData.hasStallSeconds && regularDelay >= 0) {
                    allStallSecondTimers[leagueName] = launchTimerTask(regularDelay, leagueName, _expiredStallSecondEvents)
                }
            }
        }
    }

    private fun launchTimerTask(delay: Long, leagueName: String, eventFlow: MutableSharedFlow<String>): Job =
        timerScope.launch {
            delay(delay.milliseconds)
            withContext(NonCancellable) {
                eventFlow.emit(leagueName)
            }
        }

    fun disableTimer(leagueName: String, timerRelated: TimerRelated) {
        timerRelated.cooldown = -1
        timerRelated.regularCooldown = -1
        cancelTimer(leagueName)
    }

    fun getCurrentTimerMessage(timerConfig: DraftTimer?, timerData: TimerRelated, idx: Int): K18nMessage {
        return b {
            buildString {
                append(
                    K18n_League.TimeUntil(
                        formatTimeFormatBasedOnDistance(
                            timerData.regularCooldown, timerConfig?.stallSeconds
                        )
                    )()
                )
                if (timerData.stallSecondsActive()) {
                    append(
                        K18n_League.TimeUntilStallSeconds(
                            formatTimeFormatBasedOnDistance(
                                timerData.cooldown, timerConfig?.stallSeconds
                            )
                        )()
                    )
                }
            }

        }
    }

    private fun formatTimeFormatBasedOnDistance(cooldown: Long, stallSeconds: Int?) = buildString {
        val delay = cooldown - clock.now().toEpochMilliseconds()
        if (delay >= 24 * 3600 * 1000) append(
            dayTimeFormat.format(
                Instant.fromEpochMilliseconds(cooldown).toJavaInstant()
            )
        ).append(" ")
        append(
            (if (stallSeconds == 0 && delay > SECONDS_THRESHOLD) timeFormat else timeFormatSecs).format(
                Instant.fromEpochMilliseconds(cooldown).toJavaInstant()
            )
        )
    }
}

package de.tectoast.emolga.database.league


import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.invoke
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant

private val dayTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val timeFormatSecs = DateTimeFormatter.ofPattern("HH:mm:ss")

private const val SECONDS_THRESHOLD = 15 * 60 * 1000

@Single
class DraftTimerService(val clock: Clock) {
    fun getCurrentTimerMessage(ctx: DraftRunContext, idx: Int): K18nMessage {
        val timerData = ctx.league.draftData.timer
        val timerConfig = ctx.config.timer
        return b {
            buildString {
                append(K18n_League.TimeUntil(
                    formatTimeFormatBasedOnDistance(
                        timerData.regularCooldown, timerConfig?.stallSeconds
                    )
                )())
                if(timerData.stallSecondsActive()) {
                    append(K18n_League.TimeUntilStallSeconds(
                        formatTimeFormatBasedOnDistance(
                            timerData.cooldown, timerConfig?.stallSeconds
                        )
                    )())
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
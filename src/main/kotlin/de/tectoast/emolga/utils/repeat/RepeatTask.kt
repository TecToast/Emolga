package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.features.draft.TipGameManager
import de.tectoast.emolga.utils.TimeUtils
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.ASLCoach
import de.tectoast.emolga.utils.json.emolga.draft.NDSML
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalAmount

class RepeatTask(
    lastExecution: Instant,
    amount: Int,
    difference: TemporalAmount,
    printDelays: Boolean = false,
    consumer: suspend (Int) -> Unit,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        val now = Instant.now()
        if (lastExecution.isAfter(now)) {
            var last = lastExecution
            var currAmount = amount
            while (!last.isBefore(now)) {
                val finalCurrAmount = currAmount
                val delay = last.toEpochMilli() - now.toEpochMilli()
                if (printDelays) System.out.printf("%d -> %d%n", currAmount, delay)
                scope.launch {
                    delay(delay)
                    consumer(finalCurrAmount)
                }
                currAmount--
                last -= difference
            }
        } else {
            println("LastExecution is in the past, RepeatTask will be terminated")
        }
    }

    fun clear() {
        scope.cancel("Clear called")
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        suspend fun setupRepeatTasks() {
            setupManualRepeatTasks()
            db.drafts.find().toFlow().collect { l ->
                l.takeIf { it.docEntry != null }?.tipgame?.let { tip ->
                    val duration = Duration.ofSeconds(TimeUtils.parseShortTime(tip.interval).toLong())
                    logger.info("Draft ${l.leaguename} has tipgame with interval ${tip.interval} and duration $duration")
                    RepeatTask(
                        tip.lastSending.toInstant(), tip.amount, duration, true
                    ) { TipGameManager.executeTipGameSending(db.league(l.leaguename), it) }
                    RepeatTask(
                        tip.lastLockButtons.toInstant(), tip.amount, duration, true
                    ) { TipGameManager.executeTipGameLockButtons(db.league(l.leaguename), it) }
                }
            }

        }

        private fun setupManualRepeatTasks() {
//            NDS.setupRepeatTasks()
            ASLCoach.setupRepeatTasks()
            NDSML.setupRepeatTasks()
        }
    }
}

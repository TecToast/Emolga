package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.features.draft.TipGameManager
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.defaultTimeFormat
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.ASLCoach
import de.tectoast.emolga.utils.json.emolga.draft.NDSML
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging
import java.util.*
import kotlin.time.Duration

class RepeatTask(
    lastExecution: Instant,
    amount: Int,
    difference: Duration,
    printTimestamps: Boolean = false,
    consumer: suspend (Int) -> Unit,
) {
    private val scope = createCoroutineScope("RepeatTask")
    val allTimestamps = mutableListOf<Long>()

    constructor(
        lastExecution: String,
        amount: Int,
        difference: Duration,
        printDelays: Boolean = false,
        consumer: suspend (Int) -> Unit,
    ) : this(
        Instant.fromEpochMilliseconds(defaultTimeFormat.parse(lastExecution).time),
        amount,
        difference,
        printDelays,
        consumer
    )

    init {
        val now = Clock.System.now()
        val nowM = now.toEpochMilliseconds()
        val (days, hours, minutes, seconds) = difference.toComponents { days, hours, minutes, seconds, _ ->
            listOf(-days.toInt(), -hours, -minutes, -seconds)
        }
        if (lastExecution > now) {
            val last = Calendar.getInstance().apply { timeInMillis = lastExecution.toEpochMilliseconds() }
            var currAmount = amount
            while (last.timeInMillis >= nowM && currAmount > 0) {
                val curTime = last.timeInMillis
                allTimestamps += curTime
                val finalCurrAmount = currAmount
                val delay = curTime - nowM
                if (printTimestamps) System.out.printf("%d -> %d%n", currAmount, curTime)
                scope.launch {
                    delay(delay)
                    consumer(finalCurrAmount)
                }
                currAmount--
                last.add(Calendar.DAY_OF_YEAR, days)
                last.add(Calendar.HOUR_OF_DAY, hours)
                last.add(Calendar.MINUTE, minutes)
                last.add(Calendar.SECOND, seconds)
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
                    val duration = tip.interval.toDuration()
                    logger.info("Draft ${l.leaguename} has tipgame with interval ${tip.interval} and duration $duration")
                    RepeatTask(
                        tip.lastSending, tip.amount, duration, true
                    ) { TipGameManager.executeTipGameSending(l.refresh(), it) }
                    RepeatTask(
                        tip.lastLockButtons, tip.amount, duration, true
                    ) { TipGameManager.executeTipGameLockButtons(l.refresh(), it) }
                }
                l.replayDataStore?.let { data ->
                    val size = l.battleorder[1]?.size ?: return@let
                    repeat(size) { battle ->
                        RepeatTask(
                            data.lastUploadStart + data.intervalBetweenMatches.toDuration() * battle,
                            data.amount,
                            data.intervalBetweenGD.toDuration(),
                            true
                        ) { gameday ->
                            val league = l.refresh()
                            val dataStore = league.replayDataStore ?: return@RepeatTask
                            dataStore.data[gameday]?.get(battle)?.let { league.docEntry?.analyseWithoutCheck(it) }
                                ?: throw IllegalStateException("No replay found for gameday $gameday and battle $battle")
                        }
                    }
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

package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.defaultTimeFormat
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.VideoProvideStrategy
import de.tectoast.emolga.utils.repeat.RepeatTaskType.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class RepeatTask(
    val leaguename: String,
    val type: RepeatTaskType,
    lastExecution: Instant,
    amount: Int,
    difference: Duration,
    printTimestamps: Boolean = false,
    consumer: suspend (Int) -> Unit,
) {
    private val scope = createCoroutineScope("RepeatTask")
    val taskTimestamps = TreeMap<Instant, Int>()

    constructor(
        leaguename: String,
        type: RepeatTaskType,
        lastExecution: String,
        amount: Int,
        difference: Duration,
        printDelays: Boolean = false,
        consumer: suspend (Int) -> Unit,
    ) : this(
        leaguename,
        type,
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
                val finalCurrAmount = currAmount
                taskTimestamps[Instant.fromEpochMilliseconds(curTime)] = finalCurrAmount
                val delay = curTime - nowM
                if (printTimestamps) logger.debug("{} -> {}", currAmount, curTime)
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
            allTasks.getOrPut(leaguename) { mutableMapOf() }[type] = this
        } else {
            logger.info("LastExecution is in the past, RepeatTask will be terminated")
        }
    }

    fun clear() {
        scope.cancel("Clear called")
    }

    fun findNearestTimestamp(now: Instant = Clock.System.now()): Int? {
        return taskTimestamps.ceilingEntry(now - 5.hours)?.value
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val allTasks = mutableMapOf<String, MutableMap<RepeatTaskType, RepeatTask>>()
        fun getTask(leaguename: String, type: RepeatTaskType) = allTasks[leaguename]?.get(type)
        suspend fun setupRepeatTasks() {
            db.drafts.find().toFlow().collect { l ->
                val name = l.leaguename
                suspend fun refresh() = db.league(name)
                l.setupRepeatTasks()
                l.tipgame?.let { tip ->
                    val duration = tip.interval
                    logger.info("Draft $name has tipgame with interval ${tip.interval} and duration $duration")
                    RepeatTask(
                        name, TipGameSending, tip.lastSending, tip.amount, duration, false
                    ) { refresh().executeTipGameSending(it) }
                    tip.lastLockButtons?.let { last ->
                        RepeatTask(
                            name, TipGameLockButtons, last, tip.amount, duration, false
                        ) { refresh().executeTipGameLockButtons(it) }
                    }
                }
                l.replayDataStore?.let { data ->
                    val size = l.battleorder[1]?.size ?: return@let
                    repeat(size) { battle ->
                        RepeatTask(
                            name,
                            BattleRegister,
                            data.lastUploadStart + data.intervalBetweenMatches * battle,
                            data.amount,
                            data.intervalBetweenGD,
                            false,
                        ) { gameday ->
                            val league = refresh()
                            var shouldDelay = false
                            league.tipgame?.let { _ ->
                                league.executeTipGameLockButtonsIndividual(gameday, battle)
                                shouldDelay = true
                            }
                            val dataStore = league.replayDataStore ?: return@RepeatTask
                            dataStore.data[gameday]?.get(battle)?.let {
                                it.ytVideoSaveData.enabled = true
                                val shouldSave = !it.checkIfBothVideosArePresent(league)
                                if (shouldDelay) delay(2000)
                                league.docEntry?.analyseWithoutCheck(it)
                                if (shouldSave)
                                    league.save("RepeatTaskYT")
                            }
                                ?: throw IllegalStateException("No replay found for gameday $gameday and battle $battle")
                        }
                        logger.info("YTSendChannel ${l.leaguename} $battle")
                        l.ytSendChannel?.let { ytTC ->
                            RepeatTask(
                                name,
                                YTSend,
                                data.lastUploadStart + data.intervalBetweenMatches * battle + data.intervalBetweenUploadAndVideo,
                                data.amount,
                                data.intervalBetweenGD,
                                true,
                            ) { gameday ->
                                League.executeOnFreshLock({ refresh() }) {
                                    executeYoutubeSend(ytTC, gameday, battle, VideoProvideStrategy.Fetch)
                                }
                            }
                        }
                    }
                    data.lastReminder?.let { last ->
                        RepeatTask(name, LastReminder, last.lastSend, data.amount, data.intervalBetweenGD) { gameday ->
                            jda.getTextChannelById(last.channel)!!
                                .sendMessage(refresh().buildStoreStatus(gameday)).queue()
                        }
                    }
                }
            }
        }
    }
}

sealed interface RepeatTaskType {
    data object TipGameSending : RepeatTaskType
    data object TipGameLockButtons : RepeatTaskType
    data object BattleRegister : RepeatTaskType
    data object YTSend : RepeatTaskType
    data object LastReminder : RepeatTaskType
    data class Other(val descriptor: String) : RepeatTaskType
}

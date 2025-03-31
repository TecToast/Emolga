package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.VideoProvideStrategy
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.defaultTimeFormat
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.repeat.RepeatTaskType.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import mu.KotlinLogging
import java.util.*
import kotlin.time.Duration

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
                if (printTimestamps) logger.info("{} -> {}", currAmount, defaultTimeFormat.format(curTime))
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
            logger.info("LastExecution is in the past, RepeatTask will be terminated {} {}", leaguename, type)
        }
    }

    fun clear() {
        scope.cancel("Clear called")
    }

    fun findGamedayOfDay(): Int? {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val now = Instant.fromEpochMilliseconds(cal.timeInMillis)
        val entry = taskTimestamps.ceilingEntry(now)
        return entry?.value?.takeIf { entry.key.daysUntil(now, TimeZone.UTC) == 0 }
    }

    fun findGamedayOfWeek(): Int? {
        return taskTimestamps.ceilingEntry(Clock.System.now())?.value
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val allTasks = mutableMapOf<String, MutableMap<RepeatTaskType, RepeatTask>>()
        fun getTask(leaguename: String, type: RepeatTaskType) = allTasks[leaguename]?.get(type)
        suspend fun setupRepeatTasks() {
            db.league.find().toFlow().collect { l ->
                val name = l.leaguename
                l.setupRepeatTasks()
                l.config.tipgame?.let { tip ->
                    val duration = tip.interval
                    logger.debug { "Draft $name has tipgame with interval ${tip.interval} and duration $duration" }
                    RepeatTask(
                        name, TipGameSending, tip.lastSending, tip.amount, duration
                    ) {
                        League.executeOnFreshLock(name) { executeTipGameSending(it) }
                    }
                    tip.lastLockButtons?.let { last ->
                        RepeatTask(
                            name, TipGameLockButtons, last, tip.amount, duration
                        ) {
                            League.executeOnFreshLock(name) { executeTipGameLockButtons(it) }
                        }
                    }
                }
                l.config.replayDataStore?.let { data ->
                    val size = l.battleorder[1]?.size ?: return@let
                    repeat(size) { battle ->
                        RepeatTask(
                            name,
                            RegisterInDoc,
                            data.lastUploadStart + data.intervalBetweenMatches * battle,
                            data.amount,
                            data.intervalBetweenGD,
                        ) { gameday ->
                            League.executeOnFreshLock(name) { executeRegisterInDoc(this, gameday, battle) }
                        }
                        l.config.youtube?.sendChannel?.let { ytTC ->
                            RepeatTask(
                                name,
                                YTSendManual,
                                data.lastUploadStart + data.intervalBetweenMatches * battle + data.intervalBetweenUploadAndVideo,
                                data.amount,
                                data.intervalBetweenGD,
                            ) { gameday ->
                                League.executeOnFreshLock(name) {
                                    val ytData =
                                        persistentData.replayDataStore.data[gameday]?.get(battle)?.ytVideoSaveData
                                            ?: return@executeOnFreshLock
                                    executeYoutubeSend(ytTC, gameday, battle, VideoProvideStrategy.Subscribe(ytData))
                                }
                            }
                        }
                    }
                    data.lastGamesMadeReminder?.let { last ->
                        RepeatTask(name, LastReminder, last.lastSend, data.amount, data.intervalBetweenGD) { gameday ->
                            League.executeOnFreshLock(name) {
                                jda.getTextChannelById(last.channel)!!
                                    .sendMessage(buildStoreStatus(gameday)).queue()
                            }
                        }
                    }
                }
            }
        }

        suspend fun executeRegisterInDoc(league: League, gameday: Int, battle: Int) {
            var shouldDelay = false
            league.config.tipgame?.let { _ ->
                league.executeTipGameLockButtonsIndividual(gameday, battle)
                shouldDelay = true
            }
            val dataStore = league.persistentData.replayDataStore
            dataStore.data[gameday]?.get(battle)?.let {
                if (league.config.youtube != null)
                    it.ytVideoSaveData.enabled = true
                it.checkIfBothVideosArePresent(league)
                if (shouldDelay) delay(2000)
                league.docEntry?.analyseWithoutCheck(listOf(it))
                league.save("RepeatTaskYT")
            }
                ?: throw IllegalStateException("No replay found for gameday $gameday and battle $battle")
        }
    }
}

sealed interface RepeatTaskType {
    data object TipGameSending : RepeatTaskType
    data object TipGameLockButtons : RepeatTaskType
    data object RegisterInDoc : RepeatTaskType
    data object YTSendManual : RepeatTaskType
    data object LastReminder : RepeatTaskType
    data class Other(val descriptor: String) : RepeatTaskType
}

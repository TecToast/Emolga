@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.features.wrc.WRCManager
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.defaultTimeFormat
import de.tectoast.emolga.utils.json.db
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.daysUntil
import mu.KotlinLogging
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class RepeatTask(
    val leaguename: String,
    val type: RepeatTaskType,
    lastExecution: Instant,
    amount: Int,
    difference: Duration,
    printTimestamps: Boolean = false,
    val consumer: suspend RepeatTaskData.(Int) -> Unit,
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
        consumer: suspend RepeatTaskData.(Int) -> Unit,
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
                    RepeatTaskData(Instant.fromEpochMilliseconds(curTime)).consumer(finalCurrAmount)
                }
                currAmount--
                last.add(Calendar.DAY_OF_YEAR, days)
                last.add(Calendar.HOUR_OF_DAY, hours)
                last.add(Calendar.MINUTE, minutes)
                last.add(Calendar.SECOND, seconds)
            }
            allTasks.getOrPut(leaguename) { mutableMapOf() }[type] = this
            // TODO: currently, only the last entry of a day is saved here, which is not ideal
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

        return entry?.value?.takeIf {
            entry.key.daysUntil(now, kotlinx.datetime.TimeZone.UTC) == 0
        }
    }

    fun findGamedayOfWeek(): Int? {
        return taskTimestamps.ceilingEntry(Clock.System.now())?.value
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val allTasks = mutableMapOf<String, MutableMap<RepeatTaskType, RepeatTask>>()
        fun getTask(leaguename: String, type: RepeatTaskType) = allTasks[leaguename]?.get(type)
        suspend fun setupRepeatTasks() {
            setupLeagueRepeatTasks()
            WRCManager.setupRepeatTasks()
        }

        suspend fun setupLeagueRepeatTasks() {
            db.league.find().toFlow().collect { l ->
                l.setupRepeatTasks()
            }
        }


        fun enableYTForGame(league: League, gameday: Int, battle: Int) {
            val dataStore = league.persistentData.replayDataStore
            dataStore.data[gameday]?.get(battle)?.let {
                if (league.config.youtube != null) it.ytVideoSaveData.enabled = true
            } ?: logger.warn("YT: No replay found for gameday $gameday and battle $battle in ${league.leaguename}")
        }

        suspend fun executeRegisterInDoc(league: League, gameday: Int, battle: Int) {
            val dataStore = league.persistentData.replayDataStore
            dataStore.data[gameday]?.get(battle)?.let {
                league.docEntry?.analyseWithoutCheck(listOf(it))
                league.save()
            }
                ?: logger.warn("Register: No replay found for gameday $gameday and battle $battle in ${league.leaguename}")
        }
    }
}

data class RepeatTaskData(val executionTime: Instant)

sealed interface RepeatTaskType {
    data object TipGameSending : RepeatTaskType
    data object TipGameLockButtons : RepeatTaskType
    data object RegisterInDoc : RepeatTaskType
    data object YTSendManual : RepeatTaskType
    data object YTEnable : RepeatTaskType
    data object SendReminderToParticipants : RepeatTaskType
    data object LastReminder : RepeatTaskType
    data class Other(val descriptor: String) : RepeatTaskType
}

package de.tectoast.emolga.domain.scheduling.repeat.service

import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.instanttomidnight.InstantToMidnightConverter
import kotlinx.coroutines.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import mu.KotlinLogging
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant


@Single
class RepeatTaskSchedulerService(
    private val clock: Clock,
    private val instantToMidnightConverter: InstantToMidnightConverter,
    baseScope: CoroutineScope
) : RepeatTaskScheduler {
    private val scope = baseScope + CoroutineName("TaskSchedulerService")
    private val tasks = ConcurrentHashMap<RepeatTaskType, RepeatTask>()
    private val logger = KotlinLogging.logger {}

    override fun schedule(task: RepeatTask, action: suspend (Int) -> Unit) {
        tasks[task.type] = task
        scope.launch {
            while (isActive) {
                val now = clock.now()
                val nextExecution = task.calculateNextExecution(now) ?: break
                if(task.printTimestamps) {
                    logger.info { "Next execution of ${task.type} is count ${nextExecution.count} at ${nextExecution.time}" }
                }
                delay(nextExecution.time - now)
                action(nextExecution.count)
                delay(1.seconds) // prevent multiple executions at the same time
            }
        }
    }

    override fun getUpcomingNumber(type: RepeatTaskType): Int? {
        val task = tasks[type] ?: return null
        return task.calculateNextExecution(clock.now())?.count
    }

    override fun getNumberOfToday(type: RepeatTaskType): Int? {
        val task = tasks[type] ?: return null
        val now = clock.now()
        val midnight = instantToMidnightConverter.toTodaysMidnight(now)
        val nextExecution = task.calculateNextExecution(midnight) ?: return null
        if (nextExecution.time.daysUntil(now, TimeZone.UTC) != 0) return null
        return nextExecution.count
    }

    private fun RepeatTask.calculateNextExecution(now: Instant): NextExecution? {
        if (now > lastExecution) return null
        var targetTime = lastExecution
        var count = amount
        while (targetTime - interval > now && (count > max(skipFirstN, 0) + 1)) {
            targetTime -= interval
            count--
        }
        return NextExecution(targetTime, count)
    }

    private data class NextExecution(val time: Instant, val count: Int)
}









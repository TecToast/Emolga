package de.tectoast.emolga.domain.scheduling.repeat.service

import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType
import de.tectoast.emolga.domain.scheduling.repeat.service.instanttomidnight.InstantToMidnightConverter
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant


@Single
class RepeatTaskSchedulerService(
    private val clock: Clock,
    private val instantToMidnightConverter: InstantToMidnightConverter,
    dispatcher: CoroutineDispatcher
) : RepeatTaskScheduler {
    private val scope = createCoroutineScope("TaskSchedulerService", dispatcher)
    private val tasks = ConcurrentHashMap<RepeatTaskType, RepeatTask>()

    override fun schedule(task: RepeatTask, action: suspend (Int) -> Unit) {
        tasks[task.type] = task
        scope.launch {
            while (isActive) {
                val now = clock.now()
                val nextExecution = task.calculateNextExecution(now) ?: break
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
        while (targetTime - interval > now) {
            targetTime -= interval
            count--
        }
        return NextExecution(targetTime, count)
    }

    private data class NextExecution(val time: Instant, val count: Int)
}









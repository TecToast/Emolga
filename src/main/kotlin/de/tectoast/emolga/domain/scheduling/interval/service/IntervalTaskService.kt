package de.tectoast.emolga.domain.scheduling.interval.service

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTask
import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTaskKey
import de.tectoast.emolga.domain.scheduling.interval.repository.IntervalTaskRepository
import de.tectoast.emolga.domain.scheduling.interval.service.provider.IntervalTaskProvider
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock

@Single
class IntervalTaskService(
    private val repository: IntervalTaskRepository,
    val clock: Clock,
    tasks: List<IntervalTaskProvider>,
    dispatcher: CoroutineDispatcher,
) : StartupTask {
    val scope = createCoroutineScope("IntervalTaskService", dispatcher)
    private val jobs = ConcurrentHashMap<IntervalTaskKey, Job>()
    private val tasksById = tasks.associate { it.key to it.provideTask() }

    override suspend fun onStartup() {
        tasksById.forEach { (key, task) -> addTask(key, task) }
    }

    private fun addTask(key: IntervalTaskKey, task: IntervalTask) {
        scope.launch {
            val data = repository.getTask(key)
            val now = clock.now()
            data?.notAfter?.let {
                if (now > it) return@launch
            }
            delay((data?.nextExecution ?: now) - now)
            task.consumer()
            while (true) {
                val loopNow = clock.now()
                val nextLastExecution = loopNow + task.delay
                repository.upsertTask(key, nextLastExecution)
                repository.getTask(key)?.notAfter?.let {
                    if (nextLastExecution > it) return@launch
                }
                delay(task.delay)
                task.consumer()
            }
        }
    }

    fun restartTask(key: IntervalTaskKey) {
        jobs[key]?.cancel()
        val task = tasksById[key] ?: return
        addTask(key, task)
    }
}
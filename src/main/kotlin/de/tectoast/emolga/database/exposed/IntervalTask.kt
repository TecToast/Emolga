package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.ktor.setupYTSuscribtions
import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

object IntervalTasksTable : Table("interval_tasks") {
    val key = varchar("key", 50)
    val nextExecution = timestamp("next_execution")
    val notAfter = timestamp("not_after").nullable()

    override val primaryKey = PrimaryKey(key)
}

data class IntervalTaskData(val nextExecution: Instant, val notAfter: Instant?)
data class IntervalTask(val key: String, val delay: Duration, val consumer: suspend () -> Unit)

@Single
class IntervalTaskRepository(val db: R2dbcDatabase) {
    suspend fun getTask(key: String): IntervalTaskData? = suspendTransaction(db) {
        IntervalTasksTable.select(IntervalTasksTable.nextExecution, IntervalTasksTable.notAfter)
            .where { IntervalTasksTable.key eq key }
            .firstOrNull()?.toData()
    }

    suspend fun upsertTask(key: String, nextExecution: Instant) = suspendTransaction(db) {
        IntervalTasksTable.upsert {
            it[this.key] = key
            it[this.nextExecution] = nextExecution
        }
    }

    private fun ResultRow.toData(): IntervalTaskData {
        return IntervalTaskData(this[IntervalTasksTable.nextExecution], this[IntervalTasksTable.notAfter])
    }
}

@Single
class IntervalTaskService(
    val repository: IntervalTaskRepository,
    dispatcher: CoroutineDispatcher,
    val clock: Clock,
    val tasks: List<IntervalTaskProvider>
) : StartupTask {
    val scope = createCoroutineScope("IntervalTaskService", dispatcher)

    override suspend fun onStartup() {
        tasks.forEach { addTask(it.provideTask()) }
    }

    fun addTask(task: IntervalTask) {
        scope.launch {
            val data = repository.getTask(task.key)
            data?.notAfter?.let {
                if (clock.now() > it) return@launch
            }
            val now = Clock.System.now()
            delay((data?.nextExecution ?: now) - now)
            task.consumer()
            while (true) {
                val nextLastExecution = clock.now() + task.delay
                repository.upsertTask(task.key, nextLastExecution)
                repository.getTask(task.key)?.notAfter?.let {
                    if (nextLastExecution > it) return@launch
                }
                delay(nextLastExecution - Clock.System.now())
                task.consumer()
            }
        }
    }
}

interface IntervalTaskProvider {
    fun provideTask(): IntervalTask
}

@Single
class YTSubscriptionRenewal : IntervalTaskProvider {
    override fun provideTask() = IntervalTask(
        key = "YTSubscriptionRenewal",
        delay = 4.days,
        consumer = { setupYTSuscribtions() },
    )
}
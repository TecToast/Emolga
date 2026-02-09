@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.ktor.setupYTSuscribtions
import de.tectoast.emolga.utils.createCoroutineContext
import de.tectoast.emolga.utils.json.IntervalTaskData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.repeat.IntervalTaskKey.YTSubscriptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litote.kmongo.eq
import org.litote.kmongo.upsert
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class IntervalTask(
    val name: IntervalTaskKey,
    val provideNextExecution: () -> Instant,
    val consumer: suspend () -> Unit
) {
    constructor(name: IntervalTaskKey, delay: Duration, consumer: suspend () -> Unit) : this(
        name,
        { Clock.System.now() + delay },
        consumer
    )

    lateinit var job: Job

    init {
        intervalTasks[name] = this
        start()
    }

    fun start() {
        job = launch {
            val startData = db.intervaltaskdata.get(name)
            startData?.notAfter?.let {
                if (Clock.System.now() > it) return@launch
            }
            val now = Clock.System.now()
            delay((startData?.nextExecution ?: now) - now)
            consumer()
            while (true) {
                val nextLastExecution = provideNextExecution()
                val newIntervalTaskData = IntervalTaskData(
                    name = name,
                    nextExecution = nextLastExecution,
                )
                db.intervaltaskdata.updateOne(
                    IntervalTaskData::name eq name,
                    newIntervalTaskData,
                    upsert()
                )
                if (nextLastExecution > db.intervaltaskdata.get(name)!!.notAfter) break
                delay(nextLastExecution - Clock.System.now())
                consumer()
            }
        }
    }

    companion object : CoroutineScope {
        private val intervalTasks = mutableMapOf<IntervalTaskKey, IntervalTask>()
        suspend fun restartTask(name: IntervalTaskKey) {
            db.intervaltaskdata.deleteOne(IntervalTaskData::name eq name)
            intervalTasks[name]?.job?.cancel()
            intervalTasks[name]?.start()
        }

        fun setupIntervalTasks() {
            IntervalTask(YTSubscriptions, 4.days) {
                setupYTSuscribtions()
            }
        }


        override val coroutineContext = createCoroutineContext("IntervalTask")
    }
}

enum class IntervalTaskKey {
    YTSubscriptions
}
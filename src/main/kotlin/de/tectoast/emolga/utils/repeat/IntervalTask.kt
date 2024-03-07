package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.ktor.setupYTSuscribtions
import de.tectoast.emolga.utils.createCoroutineContext
import de.tectoast.emolga.utils.json.IntervalTaskData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.litote.kmongo.eq
import org.litote.kmongo.set
import org.litote.kmongo.setTo
import org.litote.kmongo.upsert
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class IntervalTask(name: String, delay: Duration, consumer: suspend () -> Unit) {

    init {
        launch {
            val startData = db.intervaltaskdata.get(name)
            val now = Clock.System.now()
            delay((startData?.nextExecution ?: now) - now)
            consumer()
            while (true) {
                val nextLastExecution = Clock.System.now() + delay
                db.intervaltaskdata.updateOne(
                    IntervalTaskData::name eq name,
                    set(IntervalTaskData::nextExecution setTo nextLastExecution),
                    upsert()
                )
                if (nextLastExecution > db.intervaltaskdata.get(name)!!.notAfter) break
                delay(delay)
                consumer()
            }
        }
    }

    companion object : CoroutineScope {
        fun setupIntervalTasks() {
            IntervalTask("YTSubscribtions", 4.days) {
                setupYTSuscribtions()
            }
        }

        override val coroutineContext = createCoroutineContext("IntervalTask")
    }
}

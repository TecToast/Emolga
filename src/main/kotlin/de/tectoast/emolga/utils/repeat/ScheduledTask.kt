package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.features.wrc.WRCManager
import de.tectoast.emolga.ktor.InstantAsDateSerializer
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.mdb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
sealed class ScheduledTask {
    @Serializable(with = InstantAsDateSerializer::class)
    abstract val timestamp: Instant
    abstract suspend fun execute()

    @Serializable
    @SerialName("WRCFollowUp")
    data class WRCFollowUp(override val timestamp: Instant, val wrcName: String, val gameday: Int, val tryCount: Int) :
        ScheduledTask() {
        override suspend fun execute() {
            WRCManager.executeTeamSubmitClose(this)
        }
    }

    companion object {
        private val scope = createCoroutineScope("ScheduledTask")
        suspend fun setup() {
            mdb.scheduledtask.find().toFlow().collect {
                launchTask(it)
            }
        }

        suspend fun addTask(task: ScheduledTask) {
            mdb.scheduledtask.insertOne(task)
            launchTask(task)
        }

        private fun launchTask(task: ScheduledTask) {
            scope.launch {
                delay(task.timestamp - Clock.System.now())
                task.execute()
            }
        }
    }
}
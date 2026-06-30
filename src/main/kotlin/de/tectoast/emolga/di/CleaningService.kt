package de.tectoast.emolga.di

import de.tectoast.emolga.utils.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Single
class CleaningService(private val tasks: List<CleanupTask>, private val clock: Clock, dispatcher: CoroutineDispatcher) :
    StartupTask {
    val scope = createCoroutineScope("CleaningService", dispatcher)

    override suspend fun onStartup() {
        scope.launch {
            while (isActive) {
                val now = clock.now()
                tasks.forEach { it.cleanup(now) }
                delay(1.days)
            }
        }
    }
}

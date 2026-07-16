package de.tectoast.emolga.di

import kotlinx.coroutines.*
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Single
class CleaningService(
    private val tasks: List<CleanupTask>,
    private val clock: Clock,
    baseScope: CoroutineScope
) :
    StartupTask {
    val scope = baseScope + CoroutineName("CleaningService")

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

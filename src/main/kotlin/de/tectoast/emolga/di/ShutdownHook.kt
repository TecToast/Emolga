package de.tectoast.emolga.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import kotlin.concurrent.thread

@Single
class ShutdownHook(val scope: CoroutineScope) : StartupTask {
    override suspend fun onStartup() {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            runBlocking {
                scope.coroutineContext[Job]?.cancelAndJoin()
            }
        })
    }
}

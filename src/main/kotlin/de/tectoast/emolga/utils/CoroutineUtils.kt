package de.tectoast.emolga.utils

import kotlinx.coroutines.*
import mu.KotlinLogging

private val defaultCoroutineLogger = KotlinLogging.logger("DefaultCoroutineLogger")
private val basicCoroutineContext
    get() = SupervisorJob() + CoroutineExceptionHandler { ctx, t ->
        val name = ctx[CoroutineName]?.name ?: "Unknown"
        defaultCoroutineLogger.error(t) { "Error in $name" }
    }

fun createCoroutineScope(name: String, dispatcher: CoroutineDispatcher = Dispatchers.Default) =
    CoroutineScope(createCoroutineContext(name, dispatcher))

fun createCoroutineContext(name: String, dispatcher: CoroutineDispatcher = Dispatchers.Default) =
    basicCoroutineContext + dispatcher + CoroutineName(name)
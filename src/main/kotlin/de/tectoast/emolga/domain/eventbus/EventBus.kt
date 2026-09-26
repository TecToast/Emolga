package de.tectoast.emolga.domain.eventbus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

@Single
class EventBus {
    private val _flow = MutableSharedFlow<EmolgaEvent>(extraBufferCapacity = Int.MAX_VALUE)
    val flow = _flow.asSharedFlow()

    fun emit(event: EmolgaEvent) {
        _flow.tryEmit(event)
    }

    inline fun <reified T : EmolgaEvent> collect(
        scope: CoroutineScope,
        crossinline collector: suspend (T) -> Unit
    ): Job {
        return flow.filterIsInstance<T>().onEach { collector(it) }.launchIn(scope)
    }

    suspend inline fun <reified T : EmolgaEvent> collectSuspending(crossinline collector: suspend (T) -> Unit) {
        flow.filterIsInstance<T>().collect { collector(it) }
    }
}
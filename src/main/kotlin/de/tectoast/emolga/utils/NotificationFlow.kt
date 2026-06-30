package de.tectoast.emolga.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class NotificationFlow<T>(extraBufferCapacity: Int = 10) {
    private val flow = MutableSharedFlow<T>(extraBufferCapacity)

    suspend fun emit(msg: T) {
        flow.emit(msg)
    }

    fun tryEmit(msg: T) {
        flow.tryEmit(msg)
    }

    suspend fun collect(block: suspend (T) -> Unit) {
        flow.collect(block)
    }

    fun launch(scope: CoroutineScope, block: suspend (T) -> Unit) {
        flow.onEach(block).launchIn(scope)
    }
}

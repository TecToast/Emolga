package de.tectoast.emolga.utils.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("UNCHECKED_CAST")
abstract class Cache<T> {
    protected var cached: T? = null
    protected val lock = Mutex()
    open suspend operator fun invoke(): T {
        lock.withLock {
            if (cached == null) {
                updateCachedValue()
            }
        }
        return cached as T
    }

    suspend fun updateCachedValue() {
        cached = update()
    }

    abstract suspend fun update(): T
}
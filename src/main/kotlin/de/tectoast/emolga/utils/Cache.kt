@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

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

abstract class RefreshableCache<T> : Cache<T>() {
    var lastUpdate = Instant.DISTANT_PAST
    override suspend operator fun invoke(): T {
        lock.withLock {
            val now = Clock.System.now()
            if (cached == null || shouldUpdate(now)) {
                lastUpdate = now
                updateCachedValue()
            }
        }
        return cached as T
    }

    abstract fun shouldUpdate(now: Instant): Boolean
}

class OneTimeCache<T>(initial: T? = null, val function: suspend () -> T) : Cache<T>() {
    init {
        cached = initial
    }

    override suspend fun update() = function()
}

class TimedCache<T>(val time: Duration, val function: suspend () -> T) : RefreshableCache<T>() {
    override fun shouldUpdate(now: Instant): Boolean = lastUpdate + time < now
    override suspend fun update() = function()
}

class MappedCache<S, T>(private val cache: Cache<T>, val mapper: suspend (T) -> S) : RefreshableCache<S>() {
    override fun shouldUpdate(now: Instant): Boolean {
        return if (cache is RefreshableCache) {
            cache.shouldUpdate(now) || cache.lastUpdate >= lastUpdate
        } else false
    }

    override suspend fun update(): S {
        return mapper(cache())
    }
}

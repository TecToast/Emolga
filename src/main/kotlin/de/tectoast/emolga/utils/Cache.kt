package de.tectoast.emolga.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

abstract class Cache<T> {
    protected var cached: T? = null
    open suspend operator fun invoke(): T {
        if (cached == null) {
            cached = update()
        }
        return cached!!
    }

    abstract suspend fun update(): T
}

abstract class RefreshableCache<T> : Cache<T>() {
    var lastUpdate = Instant.DISTANT_PAST
    override suspend operator fun invoke(): T {
        val now = Clock.System.now()
        if (cached == null || shouldUpdate(now)) {
            lastUpdate = now
            cached = update()
        }
        return cached!!
    }

    abstract fun shouldUpdate(now: Instant): Boolean
}

class OneTimeCache<T>(val function: suspend () -> T) : Cache<T>() {
    override suspend fun update() = function()
}

class TimedCache<T>(val time: Duration, val function: suspend () -> T) : RefreshableCache<T>() {
    override fun shouldUpdate(now: Instant): Boolean = lastUpdate + time < now
    override suspend fun update() = function()
}

class MappedCache<S, T>(private val cache: Cache<T>, val mapper: suspend (T) -> S) : RefreshableCache<S>() {
    override fun shouldUpdate(now: Instant): Boolean {
        return if (cache is RefreshableCache) cache.lastUpdate >= lastUpdate else false
    }

    override suspend fun update(): S {
        return mapper(cache())
    }
}

package de.tectoast.emolga.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

abstract class RefreshableCache<T> {
    var lastUpdate = Instant.DISTANT_PAST
    private var cached: T? = null
    suspend operator fun invoke(): T {
        val now = Clock.System.now()
        if (cached == null || shouldUpdate(now)) {
            lastUpdate = now
            cached = update()
        }
        return cached!!
    }

    abstract fun shouldUpdate(now: Instant): Boolean
    abstract suspend fun update(): T
}

class TimedCache<T>(val time: Duration, val function: suspend () -> T) : RefreshableCache<T>() {
    override fun shouldUpdate(now: Instant): Boolean = lastUpdate + time < now
    override suspend fun update() = function()
}

class MappedCache<S, T>(private val cache: RefreshableCache<T>, val mapper: suspend (T) -> S) : RefreshableCache<S>() {
    override fun shouldUpdate(now: Instant): Boolean {
        return cache.lastUpdate >= lastUpdate
    }

    override suspend fun update(): S {
        return mapper(cache())
    }
}

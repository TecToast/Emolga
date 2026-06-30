package de.tectoast.emolga.utils.cache

import kotlin.time.Clock
import kotlin.time.Instant

class MappedCache<S, T>(private val cache: RefreshableCache<T>, clock: Clock, private val mapper: suspend (T) -> S) :
    RefreshableCache<S>(clock) {
    override fun shouldUpdate(now: Instant): Boolean {
        return cache.shouldUpdate(now) || cache.lastUpdate >= lastUpdate
    }

    override suspend fun update(): S {
        return mapper(cache())
    }
}

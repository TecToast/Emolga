package de.tectoast.emolga.utils.cache

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class TimedCache<T>(private val time: Duration, clock: Clock, private val function: suspend () -> T) :
    RefreshableCache<T>(clock) {
    override fun shouldUpdate(now: Instant): Boolean = lastUpdate + time < now
    override suspend fun update() = function()
}
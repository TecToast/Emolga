package de.tectoast.emolga.utils.cache

import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

@Suppress("UNCHECKED_CAST")
abstract class RefreshableCache<T>(private val clock: Clock) : Cache<T>() {
    var lastUpdate = Instant.DISTANT_PAST
    override suspend operator fun invoke(): T {
        lock.withLock {
            val now = clock.now()
            if (cached == null || shouldUpdate(now)) {
                lastUpdate = now
                updateCachedValue()
            }
        }
        return cached as T
    }

    abstract fun shouldUpdate(now: Instant): Boolean
}
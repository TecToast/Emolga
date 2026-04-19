package de.tectoast.emolga.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min
import kotlin.time.Clock

interface RateLimiter {
    suspend fun <T> withPermit(block: suspend () -> T): T
}

class CoroutineRateLimiter(
    private val capacity: Int,
    private val tokensPerSecond: Double,
    private val clock: Clock
) {
    private val mutex = Mutex()

    private var tokens: Double = capacity.toDouble()
    private var lastRefillTime = clock.now()

    suspend fun <T> withPermit(block: suspend () -> T): T {
        mutex.withLock {
            refill()
            if (tokens < 1.0) {
                val deficit = 1.0 - tokens
                val waitTimeMs = (deficit / tokensPerSecond * 1000).toLong()

                if (waitTimeMs > 0) {
                    delay(waitTimeMs)
                    refill()
                }
            }
            tokens -= 1.0
        }
        return block()
    }

    private fun refill() {
        val now = clock.now()
        val timePassedSeconds = (now - lastRefillTime).inWholeSeconds.coerceAtLeast(0)
        val tokensToAdd = timePassedSeconds * tokensPerSecond
        tokens = min(capacity.toDouble(), tokens + tokensToAdd)
        lastRefillTime = now
    }
}
package de.tectoast.emolga.utils.ratelimiter

interface RateLimiter {
    suspend fun <T> withPermit(block: suspend () -> T): T
}
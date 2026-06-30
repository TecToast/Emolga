package de.tectoast.emolga.di

import kotlin.time.Instant

interface CleanupTask {
    suspend fun cleanup(now: Instant)
}
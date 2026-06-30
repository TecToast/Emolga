package de.tectoast.emolga.domain.league.draft.model.timer

import kotlin.time.Instant

data class DelayData(val skipTimestamp: Instant, val regularTimestamp: Instant, val now: Instant) {
    val skipDelay get() = skipTimestamp - now
    val regularDelay get() = regularTimestamp - now
    val hasStallSeconds get() = skipDelay != regularDelay
}
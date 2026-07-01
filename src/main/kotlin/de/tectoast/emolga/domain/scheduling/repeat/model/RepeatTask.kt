package de.tectoast.emolga.domain.scheduling.repeat.model

import kotlin.time.Duration
import kotlin.time.Instant

data class RepeatTask(
    val type: RepeatTaskType,
    val lastExecution: Instant,
    val amount: Int,
    val interval: Duration,
    val printTimestamps: Boolean = false,
    val skipFirstN: Int = 0
)
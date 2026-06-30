package de.tectoast.emolga.domain.scheduling.interval.model

import kotlin.time.Instant

data class IntervalTaskData(val nextExecution: Instant, val notAfter: Instant?)
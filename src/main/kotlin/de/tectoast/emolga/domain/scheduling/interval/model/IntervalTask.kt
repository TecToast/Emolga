package de.tectoast.emolga.domain.scheduling.interval.model

import kotlin.time.Duration

data class IntervalTask(val delay: Duration, val consumer: suspend () -> Unit)
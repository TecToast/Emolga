package de.tectoast.emolga.domain.scheduling.interval.service.provider

import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTask
import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTaskKey

interface IntervalTaskProvider {
    val enabled: Boolean get() = true
    val key: IntervalTaskKey
    fun provideTask(): IntervalTask
}
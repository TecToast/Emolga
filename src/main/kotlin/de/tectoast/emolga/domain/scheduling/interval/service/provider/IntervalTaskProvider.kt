package de.tectoast.emolga.domain.scheduling.interval.service.provider

import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTask
import de.tectoast.emolga.domain.scheduling.interval.model.IntervalTaskKey

interface IntervalTaskProvider {
    val key: IntervalTaskKey
    fun provideTask(): IntervalTask
}
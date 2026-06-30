package de.tectoast.emolga.domain.scheduling.repeat.service.instanttomidnight

import kotlin.time.Instant

interface InstantToMidnightConverter {
    fun toTodaysMidnight(instant: Instant): Instant
}
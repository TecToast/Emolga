package de.tectoast.emolga.domain.scheduling.repeat.service.instanttomidnight

import de.tectoast.emolga.utils.toJavaLocalDateTime
import de.tectoast.emolga.utils.toKotlinInstant
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single
class LocalDateTimeInstantToMidnightConverter : InstantToMidnightConverter {
    override fun toTodaysMidnight(instant: Instant): Instant {
        return instant.toJavaLocalDateTime()
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toKotlinInstant()
    }
}
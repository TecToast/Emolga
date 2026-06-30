package de.tectoast.emolga.domain.guildspecific.flegmon.birthday.service.datetime

import de.tectoast.emolga.domain.guildspecific.flegmon.birthday.model.DayMonthYear
import de.tectoast.emolga.utils.toJavaLocalDateTime
import de.tectoast.emolga.utils.toKotlinInstant
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration

@Single
class LocalDateTimeProvider(private val clock: Clock) : DateTimeProvider {
    override fun getDayMonthYear(): DayMonthYear {
        val cal = clock.now().toJavaLocalDateTime()
        return DayMonthYear(cal.dayOfMonth, cal.monthValue, cal.year)
    }

    override fun getTimeUntilNextDay(): Duration {
        val nextMidnight =
            clock.now().toJavaLocalDateTime().plusDays(1).withHour(0).withMinute(0).withSecond(1).withNano(0)
                .toKotlinInstant()
        return nextMidnight - clock.now()
    }
}
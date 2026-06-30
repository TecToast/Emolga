package de.tectoast.emolga.domain.guildspecific.flegmon.birthday.service.datetime

import de.tectoast.emolga.domain.guildspecific.flegmon.birthday.model.DayMonthYear
import kotlin.time.Duration

interface DateTimeProvider {
    fun getDayMonthYear(): DayMonthYear
    fun getTimeUntilNextDay(): Duration
}
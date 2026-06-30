package de.tectoast.emolga.domain.guildspecific.calendar.model

import kotlin.time.Instant

data class CalendarEntry(val id: Int, val message: String, val expires: Instant)
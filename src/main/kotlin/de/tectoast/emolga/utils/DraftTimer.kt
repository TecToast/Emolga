package de.tectoast.emolga.utils

import java.util.*

@Suppress("unused")
enum class DraftTimer constructor(val timerInfo: TimerInfo, val delayInMins: Int = 120) {
    ASL(
        TimerInfo().add(10, 22, Calendar.SATURDAY, Calendar.SUNDAY)
            .add(12, 22, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
    ),
    NDS(TimerInfo().set(12, 22), 180);
}
package de.tectoast.emolga.utils

import java.util.*

class DraftTimer(private val timerInfo: TimerInfo, private val delayInMins: Int = 120) {

    /*ASL(
        TimerInfo().add(10, 22, Calendar.SATURDAY, Calendar.SUNDAY)
            .add(12, 22, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
    ),*/
    /*ASL(TimerInfo().set(12, 22), 120),
    NDS(TimerInfo().set(10, 22), 180),
    DoR(TimerInfo().set(12, 22), 60);*/

    fun calc(timerStart: Long? = null): Long {
        val cal = Calendar.getInstance()
        val ctm = cal.timeInMillis
        cal.timeInMillis = timerStart?.let { if (it > ctm) it else ctm } ?: ctm
        var elapsedMinutes = delayInMins
        while (elapsedMinutes > 0) {
            val p = timerInfo[cal[Calendar.DAY_OF_WEEK]]
            val hour = cal[Calendar.HOUR_OF_DAY]
            if (hour >= p.from && hour < p.to) elapsedMinutes-- else if (elapsedMinutes == delayInMins) cal[Calendar.SECOND] =
                0
            cal.add(Calendar.MINUTE, 1)
        }
        return cal.timeInMillis - System.currentTimeMillis()
    }
}

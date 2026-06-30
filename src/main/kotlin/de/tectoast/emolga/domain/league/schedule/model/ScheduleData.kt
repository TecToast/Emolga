package de.tectoast.emolga.domain.league.schedule.model

data class ScheduleData(val week: Int, val battleIndex: Int, val indices: List<Int>) {
    fun p1IsSecond(p1: Int) = indices[0] != p1
}

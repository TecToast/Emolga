package de.tectoast.emolga.domain.scheduling.repeat.service

import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTask
import de.tectoast.emolga.domain.scheduling.repeat.model.RepeatTaskType

interface RepeatTaskScheduler {
    fun schedule(task: RepeatTask, action: suspend (Int) -> Unit)
    fun getUpcomingNumber(type: RepeatTaskType): Int?
    fun getNumberOfToday(type: RepeatTaskType): Int?
}
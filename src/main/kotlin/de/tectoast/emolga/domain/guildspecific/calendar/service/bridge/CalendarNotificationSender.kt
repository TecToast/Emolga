package de.tectoast.emolga.domain.guildspecific.calendar.service.bridge

interface CalendarNotificationSender {
    suspend fun sendReminder(message: String)
    suspend fun updateCalendarDisplay(content: String)
}

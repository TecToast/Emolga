package de.tectoast.emolga.domain.guildspecific.calendar.service

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.domain.guildspecific.calendar.repository.CalendarRepository
import de.tectoast.emolga.domain.guildspecific.calendar.service.bridge.CalendarNotificationSender
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.format
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Single
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Instant

@Single
class CalendarService(
    private val calendarRepository: CalendarRepository,
    private val clock: Clock,
    private val notificationSender: CalendarNotificationSender,
    dispatcher: CoroutineDispatcher,
) : StartupTask {
    private val logger = KotlinLogging.logger {}
    private val scope = createCoroutineScope("CalendarService", dispatcher)
    private val calendarFormat = DateTimeFormatter.ofPattern("dd.MM. HH:mm").withZone(ZoneId.systemDefault())

    override suspend fun onStartup() {
        val entries = calendarRepository.getAllEntries()
        for (entry in entries) {
            scheduleCalendarEntry(entry.id, entry.message, entry.expires)
        }
    }

    private fun scheduleCalendarEntry(id: Int, message: String, expires: Instant) {
        scope.launch {
            delay(expires - clock.now())
            try {
                if (calendarRepository.doesntExist(id)) {
                    return@launch
                }
                notificationSender.sendReminder(message)
                calendarRepository.removeEntry(id)
                updateCalendarDisplay()
            } catch (ex: Exception) {
                logger.error(ex) { "Failed to send calendar entry" }
            }
        }
    }

    private suspend fun buildCalendar(): String {
        val allEntries = calendarRepository.getAllEntries()
        if (allEntries.isEmpty()) return "_leer_"
        return allEntries.joinToString("\n") { "**${calendarFormat.format(it.expires)}:** ${it.message}" }
    }

    suspend fun updateCalendarDisplay() {
        val content = buildCalendar()
        notificationSender.updateCalendarDisplay(content)
    }


    suspend fun scheduleNewCalendarEntry(message: String, expires: Instant) {
        val id = calendarRepository.createNewCalendarEntry(message, expires)
        scheduleCalendarEntry(id, message, expires)
    }

}
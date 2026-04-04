@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.features.various

import de.tectoast.emolga.database.exposed.CalendarRepository
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.*
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import org.koin.core.annotation.Single
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface CalendarNotificationSender {
    suspend fun sendReminder(message: String)
    suspend fun updateCalendarDisplay(content: String)
}

@Single
class JDACalendarNotificationSender(
    private val jda: JDA,
    val calendar: CalendarRepository,
    private val remindButton: RemindButton
) : CalendarNotificationSender {
    override suspend fun sendReminder(message: String) {
        val calendarTc = jda.getTextChannelById(Constants.CALENDAR_TCID) ?: return
        calendarTc.sendMessage("(<@${Constants.FLOID}>) $message")
            .addComponents(remindButton.withoutIData().into()).queue()
    }

    override suspend fun updateCalendarDisplay(content: String) {
        val calendarTc = jda.getTextChannelById(Constants.CALENDAR_TCID) ?: return
        calendarTc.editMessageById(Constants.CALENDAR_MSGID, content).queue()
    }
}

@Single
class CalendarService(
    private val calendarRepository: CalendarRepository,
    dispatcher: CoroutineDispatcher,
    private val clock: Clock,
    private val notificationSender: CalendarNotificationSender
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

    fun stop() {
        scope.cancel()
    }
}

@Single(binds = [ListenerProvider::class])
class RemindCommand(val service: CalendarService, val notificationSender: CalendarNotificationSender) :
    CommandFeature<RemindCommand.Args>(::Args, CommandSpec("remind", "remind".k18n)) {
    class Args : Arguments() {
        var date by string("Datum", "Das Datum".k18n)
        var text by string("Text", "Der Text".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val expires = TimeUtils.parseCalendarTime(e.date)
        val message = e.text
        service.scheduleNewCalendarEntry(message, Instant.fromEpochMilliseconds(expires))
        iData.reply("Reminder gesetzt!", ephemeral = true)
        service.updateCalendarDisplay()
    }
}

@Single(binds = [ListenerProvider::class])
class RemindButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("remind")) {
    override val label = "Löschen".k18n

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.reply(":D", ephemeral = true)
        iData.hook.deleteMessageById(iData.message.idLong).queue()
    }
}

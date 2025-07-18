package de.tectoast.emolga.features.various

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.database.exposed.CalendarEntry
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.TimeUtils
import de.tectoast.emolga.utils.createCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mu.KotlinLogging
import java.text.SimpleDateFormat

object CalendarSystem : CoroutineScope {
    override val coroutineContext = createCoroutineContext("CalendarSystem", Dispatchers.IO)
    private val calendarFormat = SimpleDateFormat("dd.MM. HH:mm")

    private val logger = KotlinLogging.logger {}

    fun scheduleCalendarEntry(ce: CalendarEntry) {
        launch {
            delay(ce.expires - Clock.System.now())
            try {
                dbTransaction {
                    if (runCatching { ce.refresh() }.let {
                            it.exceptionOrNull()?.let { ex -> logger.error("Failed to refresh calendar entry", ex) }
                            it.isFailure
                        }) return@dbTransaction null
                    ce.delete()
                } ?: return@launch

                val calendarTc = jda.getTextChannelById(Constants.CALENDAR_TCID)!!
                calendarTc.sendMessage("(<@${Constants.FLOID}>) ${ce.message}").setActionRow(RemindButton()).queue()
                calendarTc.editMessageById(Constants.CALENDAR_MSGID, buildCalendar()).queue()
            } catch (ex: Exception) {
                logger.error(ex) { "Failed to send calendar entry" }
            }
        }
    }


    private suspend fun buildCalendar(): String {
        return CalendarDB.getAllEntries().sortedBy { it.expires }
            .joinToString("\n") { "**${calendarFormat.format(it.expires.toEpochMilliseconds())}:** ${it.message}" }
            .ifEmpty { "_leer_" }
    }


    object RemindCommand : CommandFeature<RemindCommand.Args>(::Args, CommandSpec("remind", "remind")) {
        class Args : Arguments() {
            var date by string("Datum", "Das Datum")
            var text by string("Text", "Der Text")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val expires = TimeUtils.parseCalendarTime(e.date)
            val message = e.text
            CalendarDB.scheduleCalendarEntry(message, expires)
            iData.reply("Reminder gesetzt!", ephemeral = true)
            jda.getTextChannelById(Constants.CALENDAR_TCID)!!.editMessageById(Constants.CALENDAR_MSGID, buildCalendar())
                .queue()
        }
    }

    object RemindButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("remind")) {
        override val label = "Löschen"

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.reply(":D", ephemeral = true)
            iData.hook.deleteMessageById(iData.message.idLong).queue()
        }
    }
}

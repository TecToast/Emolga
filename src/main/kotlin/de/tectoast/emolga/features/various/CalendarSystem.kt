package de.tectoast.emolga.features.various

import de.tectoast.emolga.bot.EmolgaMain
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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.text.SimpleDateFormat

object CalendarSystem : CoroutineScope {
    override val coroutineContext = createCoroutineContext("CalendarSystem", Dispatchers.IO)
    private val calendarFormat = SimpleDateFormat("dd.MM. HH:mm")


    fun scheduleCalendarEntry(ce: CalendarEntry) {
        launch {
            delay(ce.expires.toEpochMilli() - System.currentTimeMillis())
            try {
                newSuspendedTransaction {
                    if (runCatching { ce.refresh() }.let {
                            println(
                                it.exceptionOrNull()?.stackTraceToString()
                            ); it.isFailure
                        }) return@newSuspendedTransaction null
                    ce.delete()
                } ?: return@launch

                val calendarTc: TextChannel = EmolgaMain.emolgajda.getTextChannelById(Constants.CALENDAR_TCID)!!
                calendarTc.sendMessage("(<@${Constants.FLOID}>) ${ce.message}")
                    .setActionRow(Button.primary("calendar;delete", "Löschen")).queue()
                calendarTc.editMessageById(Constants.CALENDAR_MSGID, buildCalendar()).queue()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    private fun buildCalendar(): String {
        return CalendarDB.allEntries.sortedBy { it.expires }
            .joinToString("\n") { "**${calendarFormat.format(it.expires.toEpochMilli())}:** ${it.message}" }
            .ifEmpty { "_leer_" }
    }


    object RemindCommand : CommandFeature<RemindCommand.Args>(::Args, CommandSpec("remind", "remind")) {
        class Args : Arguments() {
            var date by string("Datum", "Das Datum")
            var text by string("Text", "Der Text")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val expires = TimeUtils.parseCalendarTime(e.date)
            val message = e.text
            CalendarDB.scheduleCalendarEntry(message, expires)
            reply("Reminder gesetzt!", ephemeral = true)
            jda.getTextChannelById(Constants.CALENDAR_TCID)!!
                .editMessageById(Constants.CALENDAR_MSGID, buildCalendar()).queue()
        }
    }

    object RemindButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("remind")) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            reply(":D", ephemeral = true)
            buttonEvent {
                hook.deleteMessageById(messageId).queue()
            }
        }
    }
}

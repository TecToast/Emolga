@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.features.various

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.TimeUtils
import de.tectoast.emolga.utils.createCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object CalendarSystem : CoroutineScope {
    override val coroutineContext = createCoroutineContext("CalendarSystem", Dispatchers.IO)

    private val logger = KotlinLogging.logger {}

    fun scheduleCalendarEntry(id: Int, message: String, expires: Instant) {
        launch {
            delay(expires - Clock.System.now())
            try {
                if (CalendarDB.doesntExist(id)) {
                    return@launch
                }
                val calendarTc = jda.getTextChannelById(Constants.CALENDAR_TCID)!!
                calendarTc.sendMessage("(<@${Constants.FLOID}>) $message").setActionRow(RemindButton()).queue()
                CalendarDB.removeEntry(id)
                calendarTc.updateCalendar()
            } catch (ex: Exception) {
                logger.error(ex) { "Failed to send calendar entry" }
            }
        }
    }

    private suspend fun MessageChannel.updateCalendar() {
        editMessageById(Constants.CALENDAR_MSGID, CalendarDB.buildCalendar()).queue()
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
            CalendarDB.createNewCalendarEntry(message, expires)
            iData.reply("Reminder gesetzt!", ephemeral = true)
            jda.getTextChannelById(Constants.CALENDAR_TCID)!!.updateCalendar()
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

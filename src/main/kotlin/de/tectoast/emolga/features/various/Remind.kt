package de.tectoast.emolga.features.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants

object Remind {
    object RemindCommand : CommandFeature<RemindCommand.Args>(::Args, CommandSpec("remind", "remind")) {
        class Args : Arguments() {
            var date by string("Datum", "Das Datum")
            var text by string("Text", "Der Text")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val expires = Command.parseCalendarTime(e.date)
            val message = e.text
            CalendarDB.scheduleCalendarEntry(message, expires)
            reply("Reminder gesetzt!", ephemeral = true)
            jda.getTextChannelById(Constants.CALENDAR_TCID)!!
                .editMessageById(Constants.CALENDAR_MSGID, Command.buildCalendar()).queue()
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

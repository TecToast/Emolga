package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.CALENDAR_MSGID
import de.tectoast.emolga.utils.Constants.CALENDAR_TCID
import de.tectoast.emolga.utils.sql.managers.CalendarManager
import java.sql.Timestamp

class RemindCommand : Command("remind", "Setzt einen Reminder auf", CommandCategory.Flo, Constants.G.MY) {
    override suspend fun process(e: GuildCommandEvent) {
        try {
            val args = e.arguments
            val expires = parseCalendarTime(args.getText("date"))
            val message = args.getText("text")
            CalendarManager.insertNewEntry(message, Timestamp(expires / 1000 * 1000))
            scheduleCalendarEntry(expires, message)
            e.reply("Reminder gesetzt!", ephermal = true)
            e.jda.getTextChannelById(CALENDAR_TCID)!!.editMessageById(CALENDAR_MSGID, buildCalendar()).queue()
        } catch (ex: NumberFormatException) {
            e.reply("Das ist keine valide Zeitangabe!", ephermal = true)
            ex.printStackTrace()
        } catch (ex: Exception) {
            e.reply("Es ist ein Fehler aufgetreten!", ephermal = true)
            ex.printStackTrace()
        }
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("text", "Text", "Der Text", ArgumentManagerTemplate.Text.any())
            .add("date", "Datum", "Das Datum", ArgumentManagerTemplate.Text.any())
            .build()
        slash()
    }
}

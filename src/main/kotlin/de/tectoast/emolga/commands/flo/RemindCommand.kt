package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.CALENDAR_MSGID
import de.tectoast.emolga.utils.Constants.CALENDAR_TCID
import de.tectoast.emolga.utils.sql.managers.CalendarManager
import net.dv8tion.jda.api.entities.TextChannel
import java.sql.Timestamp

class RemindCommand : Command("remind", "Setzt einen Reminder auf", CommandCategory.Flo, Constants.MYSERVER) {
    override fun process(e: GuildCommandEvent) {
        try {
            val split = WHITESPACES_SPLITTER.split(e.message!!.contentRaw, 3)
            val expires = parseCalendarTime(split[1])
            val message = split[2]
            val calendarTc: TextChannel = e.jda.getTextChannelById(CALENDAR_TCID)!!
            CalendarManager.insertNewEntry(message, Timestamp(expires / 1000 * 1000))
            scheduleCalendarEntry(expires, message)
            e.message.delete().queue()
            calendarTc.editMessageById(CALENDAR_MSGID, buildCalendar()).queue()
        } catch (ex: NumberFormatException) {
            e.textChannel.sendMessage("Das ist keine valide Zeitangabe!").queue()
            ex.printStackTrace()
        } catch (ex: Exception) {
            e.textChannel.sendMessage("Es ist ein Fehler aufgetreten!").queue()
            ex.printStackTrace()
        }
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        /*argumentTemplate = ArgumentManagerTemplate.builder()
            .add("text", "Text", "Der Text", ArgumentManagerTemplate.Text.any())
            .add("date", "Datum", "Das Datum", ArgumentManagerTemplate.Text.any(), optional = true)
            .add("time", "Uhrzeit", "Die Uhrzeit", ArgumentManagerTemplate.Text.any(), optional = true)
            .build()
        slash(onlySlash = false)*/
    }
}
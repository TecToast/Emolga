package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants.CALENDAR_MSGID
import de.tectoast.emolga.utils.Constants.CALENDAR_TCID
import de.tectoast.emolga.utils.sql.managers.CalendarManager

class UpdateRemindersCommand : Command("updatereminders", "Updated die Reminder lol", CommandCategory.Flo) {
    override fun process(e: GuildCommandEvent) {
        calendarService.shutdownNow()
        newCalendarService()
        e.jda.getTextChannelById(CALENDAR_TCID)!!.editMessageById(CALENDAR_MSGID, buildCalendar()).queue()
        CalendarManager.allEntries.forEach { scheduleCalendarEntry(it) }
        e.done()
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }
}
package de.tectoast.emolga.commands.remind

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.sql.managers.CalendarManager
import java.sql.Timestamp
import java.util.Calendar.*

abstract class AbstractRemindCommand(name: String, val msg: String, withDateParam: Boolean) : Command(
    name, name,
    CommandCategory.Flo, Constants.G.MY
) {
    abstract fun getMillis(e: GuildCommandEvent): Long

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("text", "Text", "Der Text", ArgumentManagerTemplate.Text.any())
            if (withDateParam) {
                add("date", "Datum", "Das Datum", ArgumentManagerTemplate.Text.any())
            }
        }
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        try {
            val args = e.arguments
            val expires = getMillis(e)
            val message = args.getText("text")
            CalendarManager.insertNewEntry(message, Timestamp(expires / 1000 * 1000))
            scheduleCalendarEntry(expires, message)
            e.reply(msg, ephermal = true)
            e.jda.getTextChannelById(Constants.CALENDAR_TCID)!!
                .editMessageById(Constants.CALENDAR_MSGID, buildCalendar()).queue()
        } catch (ex: Exception) {
            e.reply("Es ist ein Fehler aufgetreten!", ephermal = true)
            ex.printStackTrace()
        }
    }
}

abstract class DatedRemindCommand(name: String, msg: String, private val dates: Set<RemindDate>) :
    AbstractRemindCommand(name, msg, false) {
    override fun getMillis(e: GuildCommandEvent) =
        System.currentTimeMillis().let { now -> dates.map { it.nextAppearance() }.filter { it - now > 0 }.min() }
}

abstract class RegularRemindCommand(name: String, msg: String) : AbstractRemindCommand(name, msg, true) {
    override fun getMillis(e: GuildCommandEvent) = parseCalendarTime(e.arguments.getText("date"))
}


data class RemindDate(val day: Int, val hour: Int, val minute: Int) {
    fun nextAppearance(): Long {
        val now = getInstance()
        now[MINUTE] = minute.also { if (minute < now[MINUTE]) now.add(HOUR_OF_DAY, 1) }
        now[HOUR_OF_DAY] = hour.also { if (hour < now[HOUR_OF_DAY]) now.add(DAY_OF_WEEK, 1) }
        now[DAY_OF_WEEK] = day.also { if (day < now[DAY_OF_WEEK]) now.add(WEEK_OF_YEAR, 1) }
        now[SECOND] = 0
        return now.timeInMillis.also { println("$this -> $it") }
    }
}

package de.tectoast.emolga.features.various.remind

import de.tectoast.emolga.domain.guildspecific.calendar.service.CalendarService
import de.tectoast.emolga.domain.util.service.TimeFormatService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RemindCommand(private val service: CalendarService, private val timeFormatService: TimeFormatService) :
    CommandFeature<RemindCommand.Args>(::Args, CommandSpec("remind", "remind".k18n)) {
    class Args : Arguments() {
        var date by string("Datum", "Das Datum".k18n)
        var text by string("Text", "Der Text".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val expires = timeFormatService.parseCalendarTime(e.date)
        val message = e.text
        service.scheduleNewCalendarEntry(message, expires)
        iData.replyRaw("Reminder gesetzt!", ephemeral = true)
        service.updateCalendarDisplay()
    }
}
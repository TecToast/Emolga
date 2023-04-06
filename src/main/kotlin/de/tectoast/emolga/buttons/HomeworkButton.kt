package de.tectoast.emolga.buttons

import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.database.exposed.CalendarEntry
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.jetbrains.exposed.sql.transactions.transaction

class HomeworkButton : ButtonListener("homework") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (name == "done") {
            e.reply(":D").setEphemeral(true).queue()
            e.hook.deleteMessageById(e.messageId).queue()
            transaction {
                CalendarEntry.find { CalendarDB.MESSAGEID eq e.messageIdLong }.firstOrNull()?.delete()
            }
        }
    }
}

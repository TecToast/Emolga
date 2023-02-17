package de.tectoast.emolga.buttons

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class CalendarButton : ButtonListener("calendar") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (name == "delete") {
            e.reply(":D").setEphemeral(true).queue()
            e.hook.deleteMessageById(e.messageId).queue()
        }
    }
}

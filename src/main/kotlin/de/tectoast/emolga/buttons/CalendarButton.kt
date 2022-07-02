package de.tectoast.emolga.buttons

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class CalendarButton : ButtonListener("calendar") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        if (name == "delete") {
            e.reply("+1").setEphemeral(true).queue()
            e.hook.deleteMessageById(e.messageId).queue()
        }
    }
}
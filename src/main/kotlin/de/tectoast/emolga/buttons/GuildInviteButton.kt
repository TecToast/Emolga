package de.tectoast.emolga.buttons

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class GuildInviteButton : ButtonListener("guildinvite") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        e.reply(e.jda.getGuildById(name)!!.defaultChannel!!.createInvite().setMaxUses(1).complete().url).queue()
    }
}
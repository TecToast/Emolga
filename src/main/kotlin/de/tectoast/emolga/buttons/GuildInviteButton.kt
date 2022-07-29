package de.tectoast.emolga.buttons

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class GuildInviteButton : ButtonListener("guildinvite") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        e.jda.getGuildById(name)
            ?.let { g ->
                e.reply(g.defaultChannel!!.createInvite().setMaxUses(1).complete().url).queue(null) {
                    e.reply(it.stackTraceToString()).queue()
                }
            } ?: e.reply("Invite failed, server not found").queue()
    }
}
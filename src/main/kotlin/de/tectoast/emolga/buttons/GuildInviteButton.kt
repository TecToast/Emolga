package de.tectoast.emolga.buttons

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class GuildInviteButton : ButtonListener("guildinvite") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        e.jda.getGuildById(name)
            ?.let { g ->
                e.reply(g.defaultChannel!!.createInvite().setMaxUses(1).await().url).queue()
            } ?: e.reply("Invite failed, server not found").queue()
    }
}
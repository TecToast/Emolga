package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.pokemon.RandomTeamCommand
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class RandomTeamButton : ButtonListener("randomteam") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (e.user.idLong != (e.message.mentions.users.firstOrNull()?.idLong)) {
            e.reply_("Das ist nicht dein Team ._.", ephemeral = true).queue()
            return
        }
        e.editMessage(RandomTeamCommand.buildString(e.user.idLong)).queue()
    }
}
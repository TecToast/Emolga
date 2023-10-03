package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.isNotFlo
import dev.minn.jda.ktx.interactions.components.Modal
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object CrinchyButton : ButtonListener("crinchy") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (e.user.isNotFlo) return
        e.replyModal(Modal("crinchy", "Crinchy-Statistik Replays") {
            paragraph(
                id = "replays",
                label = "Replays",
                required = true
            )
        }).queue()
    }
}

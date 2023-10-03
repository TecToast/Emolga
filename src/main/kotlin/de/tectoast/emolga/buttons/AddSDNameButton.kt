package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.fromFlo
import dev.minn.jda.ktx.interactions.components.Modal
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object AddSDNameButton : ButtonListener("addsdname") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        e.replyModal(Modal("addsdname", "Showdown-Namen hinzufügen") {
            short("name", "Dein Showdown-Name", required = true, requiredLength = 1..18)
            if (e.fromFlo) short("id", "Die ID", required = false)
        }).queue()
    }

}

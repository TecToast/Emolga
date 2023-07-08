package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.DBF
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class DumbestFliesButton : ButtonListener("dumbestflies") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (name == "newround") {
            DBF.endOfRound(e)

        }
    }
}

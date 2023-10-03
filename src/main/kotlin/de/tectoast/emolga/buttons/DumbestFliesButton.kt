package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.DBF
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object DumbestFliesButton : ButtonListener("dumbestflies") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (name == "newround") {
            DBF.endOfRound(e)
        }
        if (name == "question:normal") DBF.newNormalQuestion(e)
        if (name == "question:estimate") DBF.newEstimateQuestion(e)
    }
}

package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.draft.EnterResult
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ResultFinishButton : ButtonListener("resultfinish") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        EnterResult.handleFinish(e, name)
    }
}

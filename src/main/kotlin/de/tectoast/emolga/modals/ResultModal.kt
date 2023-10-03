package de.tectoast.emolga.modals

import de.tectoast.emolga.utils.draft.EnterResult
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

object ResultModal : ModalListener("result") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        EnterResult.handleModal(e)
    }
}

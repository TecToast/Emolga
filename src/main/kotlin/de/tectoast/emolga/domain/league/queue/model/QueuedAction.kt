package de.tectoast.emolga.domain.league.queue.model

import de.tectoast.emolga.domain.league.draft.model.core.DraftInput
import de.tectoast.emolga.domain.league.draft.model.core.PickInput
import de.tectoast.emolga.domain.league.draft.model.core.SwitchInput
import kotlinx.serialization.Serializable

@Serializable
data class QueuedAction(val g: QueuedMon, val y: DroppedMon? = null) {
    fun buildDraftInput(): DraftInput {
        return if (y != null) SwitchInput(y.id, g.id) else PickInput(
            g.id, g.tier.takeIf { g.tierSpecified }, g.free, g.tera
        )
    }
}
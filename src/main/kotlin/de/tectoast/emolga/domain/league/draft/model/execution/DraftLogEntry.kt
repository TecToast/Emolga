package de.tectoast.emolga.domain.league.draft.model.execution

import de.tectoast.emolga.domain.league.draft.model.core.DraftActionOrigin
import de.tectoast.emolga.domain.league.draft.model.core.DraftInput
import de.tectoast.emolga.domain.league.draft.model.core.SkipReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface DraftLogEntry {
    @Serializable
    @SerialName("Action")
    data class Action(
        val input: DraftInput,
        val type: DraftActionOrigin,
        val showTier: String?,
        val forRound: Int?,
        val byUser: Long?
    ) :
        DraftLogEntry

    @Serializable
    @SerialName("Skip")
    data class Skip(val madeUpRound: Int?, val reason: SkipReason) : DraftLogEntry

    @Serializable
    @SerialName("UserFinished")
    data object UserFinished : DraftLogEntry

}

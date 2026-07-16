package de.tectoast.emolga.domain.league.draft.model.execution

import de.tectoast.emolga.domain.league.draft.model.core.DraftActionOrigin
import de.tectoast.emolga.domain.league.draft.model.core.DraftInput
import de.tectoast.emolga.domain.league.draft.model.core.SkipReason
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant


@Serializable
sealed interface DraftLogEntry {
    val timestamp: Instant
    @Serializable
    @SerialName("Action")
    data class Action(
        val input: DraftInput,
        val origin: DraftActionOrigin,
        val showTier: String? = null,
        val forRound: Int? = null,
        val byUser: Long?,
        override val timestamp: Instant
    ) :
        DraftLogEntry

    @Serializable
    @SerialName("Skip")
    data class Skip(val madeUpRound: Int?, val reason: SkipReason, override val timestamp: Instant) : DraftLogEntry

    @Serializable
    @SerialName("UserFinished")
    data class UserFinished(override val timestamp: Instant) : DraftLogEntry

}

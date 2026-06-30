package de.tectoast.emolga.domain.league.draft.model.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SkipReason {
    @Serializable
    @SerialName("RealTimer")
    data object RealTimer : SkipReason

    @Serializable
    @SerialName("Skip")
    data class Skip(val skippedByExternal: Long? = null) : SkipReason
}

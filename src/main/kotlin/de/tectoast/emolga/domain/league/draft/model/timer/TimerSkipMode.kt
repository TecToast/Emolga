package de.tectoast.emolga.domain.league.draft.model.timer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface TimerSkipMode {

    @Serializable
    sealed interface During : TimerSkipMode {
        @Serializable
        @SerialName("NextPick")
        data object NextPick : During

        @Serializable
        @SerialName("Always")
        data object Always : During
    }

    @Serializable
    sealed interface After : TimerSkipMode {
        @Serializable
        @SerialName("AfterDraftUnordered")
        data object AfterDraftUnordered : After
    }
}

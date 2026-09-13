package de.tectoast.emolga.domain.league.tierlist.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UpdraftConfig {
    val acceptTierInput: Boolean

    @Serializable
    @SerialName("Default")
    data object Default : UpdraftConfig {
        override val acceptTierInput: Boolean = true
    }

    @Serializable
    @SerialName("OnlyWithGap")
    data class OnlyWithGap(val gap: Int) : UpdraftConfig {
        override val acceptTierInput: Boolean = true
    }

    @Serializable
    @SerialName("Disabled")
    data object Disabled : UpdraftConfig {
        override val acceptTierInput: Boolean = false
    }

    @Serializable
    @SerialName("NoCheck")
    data object NoCheck : UpdraftConfig {
        override val acceptTierInput: Boolean = true
    }
}

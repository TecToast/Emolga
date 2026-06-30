package de.tectoast.emolga.domain.league.tierlist.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UpdraftConfig {

    @Serializable
    @SerialName("Default")
    data object Default : UpdraftConfig

    @Serializable
    @SerialName("OnlyWithGap")
    data class OnlyWithGap(val gap: Int) : UpdraftConfig

    @Serializable
    @SerialName("Disabled")
    data object Disabled : UpdraftConfig

    @Serializable
    @SerialName("NoCheck")
    data object NoCheck : UpdraftConfig
}

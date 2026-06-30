package de.tectoast.emolga.domain.league.doc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DocDataProviderConfig {
    val accumulationMode: AccumulationMode get() = AccumulationMode.DEFAULT

    @Serializable
    @SerialName("WinLossLiteral")
    data class WinLossLiteral(val win: String, val loss: String) : DocDataProviderConfig {
        override val accumulationMode = AccumulationMode.NEVER
    }

    @Serializable
    @SerialName("ReplayURL")
    data class ReplayURL(val text: String?) : DocDataProviderConfig {
        override val accumulationMode = AccumulationMode.NEVER
    }

    @Serializable
    @SerialName("Kills")
    data object Kills : DocDataProviderConfig

    @Serializable
    @SerialName("Deaths")
    data object Deaths : DocDataProviderConfig

    @Serializable
    @SerialName("Alive")
    data object Alive : DocDataProviderConfig

    @Serializable
    @SerialName("Diff")
    data object Diff : DocDataProviderConfig

    @Serializable
    @SerialName("Wins")
    data object Wins : DocDataProviderConfig {
        override val accumulationMode = AccumulationMode.PER_GAME
    }

    @Serializable
    @SerialName("Losses")
    data object Losses : DocDataProviderConfig {
        override val accumulationMode = AccumulationMode.PER_GAME
    }

    @Serializable
    @SerialName("Pokemon")
    data object Pokemon : DocDataProviderConfig

    @Serializable
    @SerialName("DamageDirect")
    data object DamageDirect : DocDataProviderConfig

    @Serializable
    @SerialName("DamageIndirect")
    data object DamageIndirect : DocDataProviderConfig

    @Serializable
    @SerialName("DamageTaken")
    data object DamageTaken : DocDataProviderConfig

    @Serializable
    @SerialName("Turns")
    data object Turns : DocDataProviderConfig

}
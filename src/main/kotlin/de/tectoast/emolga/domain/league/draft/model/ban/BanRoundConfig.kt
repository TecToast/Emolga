package de.tectoast.emolga.domain.league.draft.model.ban

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface BanRoundConfig {

    @Serializable
    @SerialName("FixedTier")
    data class FixedTier(val tier: String) : BanRoundConfig

    @Serializable
    @SerialName("FixedTierSet")
    data class FixedTierSet(val tierSet: Map<String, Int>) : BanRoundConfig
}

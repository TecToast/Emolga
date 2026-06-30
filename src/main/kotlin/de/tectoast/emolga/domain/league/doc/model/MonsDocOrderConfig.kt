package de.tectoast.emolga.domain.league.doc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MonsDocOrderConfig {
    @Serializable
    @SerialName("PickOrder")
    data object PickOrder : MonsDocOrderConfig

    @Serializable
    @SerialName("TierSorted")
    data object TierSorted : MonsDocOrderConfig
}

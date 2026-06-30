package de.tectoast.emolga.domain.league.tierlist.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DraftCheck {

    @Serializable
    @SerialName("OnlyOneMega")
    data class AtmostMega(val count: Int) : DraftCheck

    @Serializable
    @SerialName("SpeciesClause")
    data object SpeciesClause : DraftCheck

    @Serializable
    @SerialName("ExactlyOneMega")
    data class ExactlyMega(val count: Int) : DraftCheck
}
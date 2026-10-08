package de.tectoast.emolga.domain.league.doc.model

import kotlinx.serialization.Serializable

@Serializable
data class HideGamesConfig(
    val weeks: Set<Int>
)

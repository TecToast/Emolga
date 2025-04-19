package de.tectoast.emolga.utils.draft

import kotlinx.serialization.Serializable

@Serializable
data class DraftPokemon(
    var name: String = "NAME",
    var tier: String = "TIER",
    var free: Boolean = false,
    var quit: Boolean = false,
    var noCost: Boolean = false
)

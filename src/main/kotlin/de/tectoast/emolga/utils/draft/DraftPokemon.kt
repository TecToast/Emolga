package de.tectoast.emolga.utils.draft

import kotlinx.serialization.Serializable

@Serializable
data class DraftPokemon(var name: String, var tier: String)
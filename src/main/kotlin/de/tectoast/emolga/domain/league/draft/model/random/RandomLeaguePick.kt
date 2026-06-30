package de.tectoast.emolga.domain.league.draft.model.random

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class RandomLeaguePick(
    val showdownId: ShowdownID,
    val tier: String,
    val free: Boolean = false,
    val tera: Boolean = false,
    val data: Map<String, String?> = mapOf(),
    val history: Set<ShowdownID> = setOf(),
    var disabled: Boolean = false
)
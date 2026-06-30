package de.tectoast.emolga.domain.league.draft.model.random

import de.tectoast.emolga.domain.pokemon.model.ShowdownID


data class RandomPickUserInput(
    val tier: String?,
    val type: String?,
    val free: Boolean = false,
    val tera: Boolean = false,
    val ignoreRestrictions: Boolean = false,
    val skipMons: Set<ShowdownID> = emptySet()
)

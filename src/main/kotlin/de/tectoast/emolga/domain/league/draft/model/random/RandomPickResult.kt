package de.tectoast.emolga.domain.league.draft.model.random

import de.tectoast.emolga.domain.pokemon.model.ShowdownID

sealed interface RandomPickResult {
    val showdownId: ShowdownID
    val tier: String

    data class RerollPossible(
        override val showdownId: ShowdownID,
        override val tier: String,
        val displayName: String,
        val jokersRemaining: Int
    ) : RandomPickResult

    data class NoReroll(override val showdownId: ShowdownID, override val tier: String) : RandomPickResult
}

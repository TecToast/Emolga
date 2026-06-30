package de.tectoast.emolga.domain.league.draft.model.core

import de.tectoast.emolga.domain.league.tierlist.model.TierData
import de.tectoast.emolga.domain.pokemon.model.ShowdownID

data class DraftAction(
    val tier: TierData,
    val showdownId: ShowdownID,
    val free: Boolean = false,
    val tera: Boolean = false,
    val switch: DraftPokemon? = null
) {

    constructor(showdownId: ShowdownID, officialTier: String) : this(
        tier = TierData(officialTier, officialTier, false), showdownId = showdownId
    )

    val officialTier: String
        get() = tier.official

    val specifiedTier: String
        get() = tier.specified
}
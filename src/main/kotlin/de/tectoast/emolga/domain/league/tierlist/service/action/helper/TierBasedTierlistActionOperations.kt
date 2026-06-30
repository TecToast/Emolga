package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionOperations

interface TierBasedTierlistActionOperations<C : TierBasedTierlistConfig> : TierlistActionOperations<C> {

    fun getSingleMap(config: C): Map<String, Int>

    fun getCurrentAvailableTiers(config: C, picks: List<DraftPokemon>): List<String>

    fun getTierInsertIndex(config: C, picks: List<DraftPokemon>): Int

    fun getPicksWithInsertOrder(config: C, picks: List<DraftPokemon>): Map<Int, DraftPokemon>
}
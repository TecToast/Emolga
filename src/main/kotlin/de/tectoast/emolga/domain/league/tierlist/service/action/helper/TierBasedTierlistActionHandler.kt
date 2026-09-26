package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler

abstract class TierBasedTierlistActionHandler<C : TierBasedTierlistConfig> :
    TierlistActionHandler<C>(),
    TierBasedTierlistActionOperations<C> {

    override fun getSortedPicks(
        config: C,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        val indexMap = getPicksWithInsertOrder(config, picks)
        return picks.indices.mapNotNull { indexMap[it] }
    }

    override fun getPicksWithInsertOrder(
        config: C,
        picks: List<DraftPokemon>
    ): Map<Int, DraftPokemon> {
        val indexMap = mutableMapOf<Int, DraftPokemon>()
        for (i in picks.indices) {
            val subList = picks.subList(0, i + 1)
            val index = getTierInsertIndex(config, subList)
            indexMap[index] = picks[i]
        }
        return indexMap
    }
}
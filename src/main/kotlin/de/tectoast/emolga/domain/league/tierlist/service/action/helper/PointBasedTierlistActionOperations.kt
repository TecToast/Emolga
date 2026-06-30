package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.tierlist.model.config.PointBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionOperations

interface PointBasedTierlistActionOperations<C : PointBasedTierlistConfig> : TierlistActionOperations<C> {
    fun getPointsForTier(config: C, tier: String): Int?

    fun getPointsForMon(config: C, pokemon: DraftPokemon): Int {
        return getPointsForTier(config, pokemon.tier)
            ?: error("Tier ${pokemon.tier} not found for pokemon ${pokemon.showdownId}")
    }

    fun getPointsOfUser(config: C, picks: List<DraftPokemon>): Int {
        return config.globalPoints - picks.sumOf {
            if (it.quit || it.noCost) 0
            else getPointsForMon(config, it)
        }
    }
}
package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.tierlist.model.config.OnlyPointBasedTierlistConfig

interface OnlyPointBasedTierlistActionOperations<C : OnlyPointBasedTierlistConfig> :
    PointBasedTierlistActionOperations<C> {
    fun getMinimumPrice(config: C): Int
}
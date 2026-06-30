package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.tierlist.model.config.PointBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler

abstract class PointBasedTierlistActionHandler<C : PointBasedTierlistConfig> : TierlistActionHandler<C>(),
    PointBasedTierlistActionOperations<C>
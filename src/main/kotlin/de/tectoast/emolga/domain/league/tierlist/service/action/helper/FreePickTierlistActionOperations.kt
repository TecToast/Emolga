package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.tierlist.model.config.FreePickTierlistConfig

interface FreePickTierlistActionOperations<C : FreePickTierlistConfig> : TierBasedTierlistActionOperations<C>,
    PointBasedTierlistActionOperations<C>
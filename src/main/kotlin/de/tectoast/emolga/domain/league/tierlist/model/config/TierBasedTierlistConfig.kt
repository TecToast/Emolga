package de.tectoast.emolga.domain.league.tierlist.model.config

import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig

interface TierBasedTierlistConfig : TierlistConfig {
    val updraftConfig: UpdraftConfig
}
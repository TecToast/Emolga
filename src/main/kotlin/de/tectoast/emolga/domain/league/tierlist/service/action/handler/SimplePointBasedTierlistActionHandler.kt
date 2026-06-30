package de.tectoast.emolga.domain.league.tierlist.service.action.handler

import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.OnlyPointBasedTierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.PointBasedTierlistActionHandler
import org.koin.core.annotation.Single

@Single(binds = [TierlistActionHandler::class, PointBasedTierlistActionHandler::class])
class SimplePointBasedTierlistActionHandler : OnlyPointBasedTierlistActionHandler<TierlistConfig.SimplePointBased>() {
    override val targetClass = TierlistConfig.SimplePointBased::class

    override fun getTiers(config: TierlistConfig.SimplePointBased) = config.prices.keys.toList()

    override fun getPointsForTier(config: TierlistConfig.SimplePointBased, tier: String) = config.prices[tier]

    override fun getMinimumPrice(config: TierlistConfig.SimplePointBased) = config.prices.values.min()
}
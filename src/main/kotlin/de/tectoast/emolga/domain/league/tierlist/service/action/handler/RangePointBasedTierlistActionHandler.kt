package de.tectoast.emolga.domain.league.tierlist.service.action.handler

import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.OnlyPointBasedTierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.PointBasedTierlistActionHandler
import org.koin.core.annotation.Single

@Single(binds = [TierlistActionHandler::class, PointBasedTierlistActionHandler::class])
class RangePointBasedTierlistActionHandler : OnlyPointBasedTierlistActionHandler<TierlistConfig.RangePointBased>() {
    override val targetClass = TierlistConfig.RangePointBased::class

    override fun getTiers(config: TierlistConfig.RangePointBased): List<String> {
        return (config.maxTier downTo config.minTier).map { it.toString() }
    }

    override fun getPointsForTier(config: TierlistConfig.RangePointBased, tier: String): Int? {
        val tierInt = tier.toIntOrNull() ?: return null
        if (tierInt !in config.minTier..config.maxTier) return null
        return tierInt
    }

    override fun getMinimumPrice(config: TierlistConfig.RangePointBased) = config.minTier
}
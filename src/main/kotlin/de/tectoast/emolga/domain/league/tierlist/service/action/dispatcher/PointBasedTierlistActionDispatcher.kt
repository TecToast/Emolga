package de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher

import de.tectoast.emolga.domain.league.tierlist.model.config.PointBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionOperations
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.PointBasedTierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.PointBasedTierlistActionOperations
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
@Suppress("UNCHECKED_CAST")
class PointBasedTierlistActionDispatcher(
    handlers: List<PointBasedTierlistActionHandler<PointBasedTierlistConfig>>,
    tierlistDispatcher: TierlistActionDispatcher
) : PointBasedTierlistActionOperations<PointBasedTierlistConfig>,
    TierlistActionOperations<PointBasedTierlistConfig> by (tierlistDispatcher as TierlistActionOperations<PointBasedTierlistConfig>) {
    private val registry = HandlerRegistry(handlers)
    override fun getPointsForTier(
        config: PointBasedTierlistConfig,
        tier: String
    ) = registry.getHandler(config).getPointsForTier(config, tier)
}
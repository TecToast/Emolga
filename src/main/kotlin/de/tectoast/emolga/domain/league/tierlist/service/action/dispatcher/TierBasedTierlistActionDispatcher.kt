package de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionOperations
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionOperations
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
@Suppress("UNCHECKED_CAST")
class TierBasedTierlistActionDispatcher(
    handlers: List<TierBasedTierlistActionHandler<TierBasedTierlistConfig>>,
    tierlistDispatcher: TierlistActionDispatcher
) : TierBasedTierlistActionOperations<TierBasedTierlistConfig>,
    TierlistActionOperations<TierBasedTierlistConfig> by (tierlistDispatcher as TierlistActionOperations<TierBasedTierlistConfig>) {

    private val registry = HandlerRegistry(handlers)

    override fun getSingleMap(config: TierBasedTierlistConfig) = registry.getHandler(config).getSingleMap(config)

    override fun getCurrentAvailableTiers(config: TierBasedTierlistConfig, picks: List<DraftPokemon>) =
        registry.getHandler(config).getCurrentAvailableTiers(config, picks)

    override fun getTierInsertIndex(
        config: TierBasedTierlistConfig,
        picks: List<DraftPokemon>
    ) = registry.getHandler(config).getTierInsertIndex(config, picks)

    override fun getSortedPicks(
        config: TierBasedTierlistConfig,
        picks: List<DraftPokemon>
    ) = registry.getHandler(config).getSortedPicks(config, picks)

    override fun getPicksWithInsertOrder(
        config: TierBasedTierlistConfig,
        picks: List<DraftPokemon>
    ) = registry.getHandler(config).getPicksWithInsertOrder(config, picks)
}
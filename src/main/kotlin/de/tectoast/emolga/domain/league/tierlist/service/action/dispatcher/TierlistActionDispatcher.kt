package de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionOperations
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class TierlistActionDispatcher(handlers: List<TierlistActionHandler<TierlistConfig>>) :
    TierlistActionOperations<TierlistConfig> {
    private val registry = HandlerRegistry(handlers)

    override fun publicTierToDBTier(
        config: TierlistConfig,
        tier: String
    ) = registry.getHandler(config).publicTierToDBTier(config, tier)

    override fun compareTiers(
        config: TierlistConfig,
        tierA: String,
        tierB: String
    ) = registry.getHandler(config).compareTiers(config, tierA, tierB)

    context(data: ValidationRelevantData)
    override suspend fun handleDraftActionWithGeneralChecks(
        config: TierlistConfig,
        action: DraftAction,
        context: DraftActionContext?
    ) = registry.getHandler(config).handleDraftActionWithGeneralChecks(config, action, context)

    override suspend fun buildAnnounceData(
        config: TierlistConfig,
        picks: List<DraftPokemon>
    ) = registry.getHandler(config).buildAnnounceData(config, picks)

    override fun getTiers(config: TierlistConfig) = registry.getHandler(config).getTiers(config)

    override fun getTiersForUpdraftCompare(config: TierlistConfig) =
        registry.getHandler(config).getTiersForUpdraftCompare(config)

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistConfig,
        idx: Int,
        currentState: List<QueuedAction>
    ) = registry.getHandler(config).checkLegalityOfQueue(config, idx, currentState)

    override fun getTierOrderingComparatorWithoutName(config: TierlistConfig) =
        registry.getHandler(config).getTierOrderingComparatorWithoutName(config)

    override fun getSortedPicks(
        config: TierlistConfig,
        picks: List<DraftPokemon>
    ) = registry.getHandler(config).getSortedPicks(config, picks)
}
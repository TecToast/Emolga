package de.tectoast.emolga.domain.league.tierlist.service.action

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.draftcheck.DraftCheckDispatcher
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.handler.BaseHandler
import org.koin.core.component.inject

abstract class TierlistActionHandler<C : TierlistConfig> : BaseHandler<C>,
    TierlistActionOperations<C> {

    private val generalCheckDispatcher: DraftCheckDispatcher by inject()

    override fun getTierOrderingComparatorWithoutName(config: C): Comparator<DraftPokemon> {
        val tierOrder = getTiers(config)
        return compareBy { tierOrder.indexOf(it.tier) }
    }

    override fun getSortedPicks(
        config: C,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        return picks.sortedWith(getTierOrderingComparatorWithoutName(config))
    }

    context(data: ValidationRelevantData)
    abstract fun handleDraftAction(config: C, action: DraftAction, context: DraftActionContext? = null): ErrorOrNull

    override fun publicTierToDBTier(config: C, tier: String) = tier
    override fun compareTiers(config: C, tierA: String, tierB: String): Int? {
        val tiers = getTiers(config)
        val indexA = tiers.indexOf(tierA)
        val indexB = tiers.indexOf(tierB)
        if (indexA == -1 || indexB == -1) return null
        return indexA - indexB
    }

    context(data: ValidationRelevantData)
    override suspend fun handleDraftActionWithGeneralChecks(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        for (check in config.draftChecks) {
            generalCheckDispatcher.check(check, action)?.let { return it }
        }
        return handleDraftAction(config, action, context)
    }

    fun Map<String, Int>.deductPicks(list: List<DraftPokemon>): Map<String, Int> {
        val map = toMutableMap()
        for (pick in list) {
            pick.takeUnless { it.free || it.quit }?.let { map.add(it.tier, -1) }
        }
        return map
    }

    override fun getTiersForUpdraftCompare(config: C) = getTiers(config)
}
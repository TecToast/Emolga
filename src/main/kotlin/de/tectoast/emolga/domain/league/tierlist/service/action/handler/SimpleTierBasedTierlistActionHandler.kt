package de.tectoast.emolga.domain.league.tierlist.service.action.handler

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single(binds = [TierlistActionHandler::class, TierBasedTierlistActionHandler::class])
class SimpleTierBasedTierlistActionHandler :
    TierBasedTierlistActionHandler<TierlistConfig.SimpleTierBased>() {

    override val targetClass = TierlistConfig.SimpleTierBased::class

    context(data: ValidationRelevantData)
    override fun handleDraftActionAfterGeneralTierCheck(
        config: TierlistConfig.SimpleTierBased,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        val options = getPossibleTiers(config, data.picks)
        if (options[action.specifiedTier]!! <= 0) {
            if (config.tiers[action.specifiedTier] == 0) {
                return K18n_Tierlist.MustUpdraft(action.specifiedTier)
            }
            if (action.switch != null) return null
            return K18n_Tierlist.CantPickTier(action.specifiedTier)
        }
        return null
    }

    private fun getPossibleTiers(config: TierlistConfig.SimpleTierBased, picks: List<DraftPokemon>) =
        config.tiers.deductPicks(picks)

    override suspend fun buildAnnounceData(
        config: TierlistConfig.SimpleTierBased,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        return getPossibleTiers(config, picks).entries.sortedBy { config.tierOrder.indexOf(it.key) }
            .filterNot { it.value == 0 }
            .joinToString { tierAmountToString(it.key, it.value) }.let {
                if (it.isEmpty()) null else K18n_League.PossibleTiers(it)
            }
    }


    override fun getTiers(config: TierlistConfig.SimpleTierBased) = config.tierOrder

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistConfig.SimpleTierBased,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val map = getPossibleTiers(config, data.picks).toMutableMap()
        currentState.forEach {
            map.add(it.g.tier, -1)
            it.y?.let { y -> map.add(y.tier, 1) }
        }
        val result = map.entries.firstOrNull { it.value < 0 }
        val isIllegal = result != null
        if (isIllegal) {
            return K18n_QueuePicks.LegalTooManyInSingleTier(result.key)
        }
        return null
    }

    override fun getSingleMap(config: TierlistConfig.SimpleTierBased) = config.tiers

    override fun getCurrentAvailableTiers(config: TierlistConfig.SimpleTierBased, picks: List<DraftPokemon>) =
        getPossibleTiers(config, picks).filter { it.value > 0 }.keys.toList()

    override fun getTierInsertIndex(
        config: TierlistConfig.SimpleTierBased,
        picks: List<DraftPokemon>
    ): Int {
        val tier = picks.lastOrNull()?.tier ?: error("No picks to determine tier for index")
        var index = 0
        for (entry in config.tiers.entries.sortedBy { config.tierOrder.indexOf(it.key) }) {
            if (entry.key == tier) {
                return picks.count { !it.free && !it.quit && it.tier == tier } + index - 1
            }
            index += entry.value
        }
        error("Tier $tier not found by")
    }

    override fun getSortedPicks(
        config: TierlistConfig.SimpleTierBased,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        return picks.sortedWith(getTierOrderingComparatorWithoutName(config))
    }
}
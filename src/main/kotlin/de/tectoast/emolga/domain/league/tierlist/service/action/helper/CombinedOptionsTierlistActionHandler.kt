package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.CombinedOptionsTierlistConfig
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.draft.K18n_Tierlist

abstract class CombinedOptionsTierlistActionHandler<C : CombinedOptionsTierlistConfig> :
    TierBasedTierlistActionHandler<C>() {

    context(data: ValidationRelevantData)
    override fun handleDraftActionAfterGeneralTierCheck(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        val specifiedTier = action.specifiedTier
        val allTiers = getAllPossibleTiers(config, data.picks)
        if (allTiers.all { map -> map.getOrDefault(specifiedTier, 0) <= 0 }) {
            if (config.combinedOptions.all { p -> p[specifiedTier] == 0 }) {
                return K18n_Tierlist.MustUpdraft(specifiedTier)
            }
            if (action.switch != null) return null
            return K18n_Tierlist.CantPickTier(specifiedTier)
        }
        return null
    }

    override fun getCurrentAvailableTiers(config: C, picks: List<DraftPokemon>): List<String> {
        return config.combinedOptions.flatMapTo(mutableSetOf()) { opt ->
            val deducted = opt.deductPicks(picks)
            if (deducted.any { it.value < 0 }) emptyList() else deducted.entries.filter { it.value > 0 }.map { it.key }
        }.sortedBy { config.tierOrder.indexOf(it) }
    }

    fun getAllPossibleTiers(config: C, picks: List<DraftPokemon>): List<Map<String, Int>> =
        config.combinedOptions.map { it.deductPicks(picks) }

    override fun getTiers(config: C): List<String> {
        return config.tierOrder
    }


    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: C,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val res = getAllPossibleTiers(config, data.picks)
        val finalMaps = res.map { map ->
            val tempMap = map.toMutableMap()
            currentState.forEach {
                tempMap.add(it.g.tier, -1)
                it.y?.let { y -> tempMap.add(y.tier, 1) }
            }
            tempMap
        }
        val isIllegal = finalMaps.all { map -> map.any { it.value < 0 } }
        if (isIllegal) {
            return K18n_QueuePicks.LegalTooManyInTier
        }
        return null
    }
}
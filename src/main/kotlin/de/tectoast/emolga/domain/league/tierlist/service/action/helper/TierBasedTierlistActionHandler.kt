package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.updraft.UpdraftConfigDispatcher
import de.tectoast.emolga.utils.ErrorOrNull
import org.koin.core.component.inject

abstract class TierBasedTierlistActionHandler<C : TierBasedTierlistConfig> :
    TierlistActionHandler<C>(),
    TierBasedTierlistActionOperations<C> {

    private val updraftConfigDispatcher: UpdraftConfigDispatcher by inject()

    context(data: ValidationRelevantData)
    abstract fun handleDraftActionAfterGeneralTierCheck(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull

    context(data: ValidationRelevantData)
    override fun handleDraftAction(
        config: C,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        if (action.specifiedTier != action.officialTier) {
            updraftConfigDispatcher.handleUpdraft(config.updraftConfig, config, action, this)?.let { return it }
        }
        return handleDraftActionAfterGeneralTierCheck(config, action, context)
    }

    override fun getSortedPicks(
        config: C,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        val indexMap = getPicksWithInsertOrder(config, picks)
        return picks.indices.map { indexMap[it]!! }
    }

    override fun getPicksWithInsertOrder(
        config: C,
        picks: List<DraftPokemon>
    ): Map<Int, DraftPokemon> {
        val indexMap = mutableMapOf<Int, DraftPokemon>()
        for (i in picks.indices) {
            val subList = picks.subList(0, i + 1)
            val index = getTierInsertIndex(config, subList)
            indexMap[index] = picks[i]
        }
        return indexMap
    }
}
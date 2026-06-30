package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.handler.tierAmountToString
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import de.tectoast.emolga.utils.invoke
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single(binds = [TierlistActionHandler::class, TierBasedTierlistActionHandler::class])
class FreePickTierlistActionHandler :
    TierBasedTierlistActionHandler<TierlistConfig.FreePick>(),
    FreePickTierlistActionOperations<TierlistConfig.FreePick> {
    override val targetClass = TierlistConfig.FreePick::class

    override fun getPointsForMon(
        config: TierlistConfig.FreePick,
        pokemon: DraftPokemon
    ): Int {
        if (!pokemon.free) return 0
        return config.pointPrices[pokemon.tier] ?: error("No point price found for tier ${pokemon.tier}")
    }

    private fun getPossibleTiers(config: TierlistConfig.FreePick, picks: List<DraftPokemon>) =
        config.normalTiers.deductPicks(picks)

    override suspend fun buildAnnounceData(
        config: TierlistConfig.FreePick,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        return getPossibleTiers(config, picks).entries.filterNot { it.value == 0 }
            .sortedBy { config.tierOrder.indexOf(it.key) }
            .joinToString { tierAmountToString(it.key, it.value) }.let {
                if (it.isEmpty()) null else K18n_League.PossibleTiers(it)
            }?.let { tierMsg ->
                b {
                    "${tierMsg()}, ${K18n_League.PossiblePoints(getPointsOfUser(config, picks))()}, Free: ${
                        config.freeAmount - picks.count { it.free && !it.quit }
                    }"
                }
            }
    }

    override fun getTiers(config: TierlistConfig.FreePick) = config.tierOrder

    override fun getTiersForUpdraftCompare(config: TierlistConfig.FreePick) =
        config.tierOrder.filter { it in config.normalTiers }

    override fun getSingleMap(config: TierlistConfig.FreePick) = config.normalTiers

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistConfig.FreePick,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val map = getPossibleTiers(config, data.picks).toMutableMap()
        val currentPoints = getPointsOfUser(config, data.picks)
        var cost = 0
        currentState.forEach {
            if (it.g.free) {
                cost += getPointsForTier(config, it.g.tier)!!
                it.y?.let { y -> cost -= getPointsForTier(config, y.tier)!! }
            } else {
                map.add(it.g.tier, -1)
                it.y?.let { y -> map.add(y.tier, 1) }
            }
        }
        val result = map.entries.firstOrNull { it.value < 0 }
        if (result != null) {
            return K18n_QueuePicks.LegalTooManyInSingleTier(result.key)
        }
        val hasMega = data.picks.any { it.free && !it.quit && getPointsForTier(config, it.tier)!! <= 0 }
        val newPoints = currentPoints - cost
        if (newPoints < 0 && (hasMega || newPoints < config.pointPrices.values.min())) {
            return K18n_Tierlist.NotEnoughPoints("$currentPoints - $cost = $newPoints < 0")
        }
        val minimumRequired = minimumNeededPointsForTeamCompletion(
            config = config,
            alreadyFree = data.picks.count { it.free && !it.quit } + currentState.count { it.g.free },
            coerceAtLeastOne = hasMega
        )
        if (newPoints < minimumRequired) {
            return K18n_Tierlist.MinimumNeededError(minimumRequired, newPoints)
        }
        return null
    }

    context(data: ValidationRelevantData)
    override fun handleDraftActionAfterGeneralTierCheck(
        config: TierlistConfig.FreePick,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        val picks = data.picks
        val options = getPossibleTiers(config, picks)
        if (action.free) {
            val currentFreeAmount = picks.count { it.free && !it.quit }
            if (currentFreeAmount >= config.freeAmount) {
                return K18n_Tierlist.NoFreePicksLeft(config.freeAmount)
            }
            val currentPoints = getPointsOfUser(config, picks)
            val cost = getPointsForTier(config, action.specifiedTier) ?: return K18n_TierNotFound(action.specifiedTier)
            val newPoints = currentPoints - cost
            val hasMega = picks.any { it.free && !it.quit && getPointsForTier(config, it.tier)!! <= 0 }
            if (newPoints < 0 && (hasMega || newPoints < config.pointPrices.values.min())) {
                return K18n_Tierlist.NotEnoughPoints("$currentPoints - $cost = $newPoints < 0")
            }
            val minimumRequired = minimumNeededPointsForTeamCompletion(
                config = config,
                alreadyFree = currentFreeAmount + 1,
                coerceAtLeastOne = hasMega
            )
            if (newPoints < minimumRequired) {
                return K18n_Tierlist.MinimumNeededError(minimumRequired, newPoints)
            }
            context?.freePick = true
            return null
        } else {
            val optionsInTier = options[action.specifiedTier] ?: return K18n_TierNotFound(action.specifiedTier)
            if (optionsInTier <= 0) {
                if (config.normalTiers[action.specifiedTier] == 0) {
                    return K18n_Tierlist.MustUpdraft(action.specifiedTier)
                }
                if (action.switch != null) return null
                return K18n_Tierlist.CantPickTier(action.specifiedTier)
            }
        }
        return null
    }

    private fun minimumNeededPointsForTeamCompletion(
        config: TierlistConfig.FreePick,
        alreadyFree: Int,
        coerceAtLeastOne: Boolean
    ): Int {
        val missingAmount = config.freeAmount - alreadyFree
        if (missingAmount <= 0) return 0
        val base = config.pointPrices.minOf {
            if (coerceAtLeastOne) it.value.coerceAtLeast(1) else it.value
        }
        return base + (missingAmount - 1) * config.pointPrices.minOf { it.value.coerceAtLeast(1) }
    }

    override fun getCurrentAvailableTiers(
        config: TierlistConfig.FreePick,
        picks: List<DraftPokemon>
    ) = getPossibleTiers(config, picks).filter { it.value > 0 }.keys.sortedBy { config.tierOrder.indexOf(it) }.toList()

    override fun getTierInsertIndex(
        config: TierlistConfig.FreePick,
        picks: List<DraftPokemon>
    ): Int {
        val relevantPicks = picks.filter { !it.free && !it.quit }
        val tier = relevantPicks.lastOrNull()?.tier ?: error("No picks to determine tier for index")
        var index = 0
        for (entry in config.normalTiers.entries.sortedBy { config.tierOrder.indexOf(it.key) }) {
            if (entry.key == tier) {
                return relevantPicks.count { !it.free && !it.quit && it.tier == tier } + index - 1
            }
            index += entry.value
        }
        error("Tier $tier not found by")
    }

    override fun getPointsForTier(
        config: TierlistConfig.FreePick,
        tier: String
    ) = config.pointPrices[tier]
}
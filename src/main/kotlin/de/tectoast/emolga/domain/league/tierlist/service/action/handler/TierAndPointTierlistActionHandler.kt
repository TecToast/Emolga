package de.tectoast.emolga.domain.league.tierlist.service.action.handler

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.PointBasedTierlistActionOperations
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
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
class TierAndPointTierlistActionHandler :
    TierBasedTierlistActionHandler<TierlistConfig.TierAndPoint>(),
    PointBasedTierlistActionOperations<TierlistConfig.TierAndPoint> {
    override val targetClass = TierlistConfig.TierAndPoint::class

    context(data: ValidationRelevantData)
    override fun handleDraftActionAfterGeneralTierCheck(
        config: TierlistConfig.TierAndPoint,
        action: DraftAction,
        context: DraftActionContext?
    ): ErrorOrNull {
        val officialCost =
            getPointsForTier(config, action.officialTier) ?: return K18n_TierNotFound(action.specifiedTier)
        val cost = if (action.tier.isTierSpecified) {
            val possibleTiers = config.tiers[action.specifiedTier]!!.tiers
            if (officialCost > possibleTiers.max()) return K18n_Tierlist.CantUpdraft(
                officialCost.toString().pointsToActualTier(config), action.specifiedTier
            )
            possibleTiers.min().coerceAtLeast(officialCost)
        } else officialCost
        val specifiedTier = cost.toString().pointsToActualTier(config)
        val options = getPossibleTiers(config, data.picks)
        if (options[specifiedTier]!! <= 0) {
            if (config.tiers[specifiedTier]?.amount == 0) {
                return K18n_Tierlist.MustUpdraft(specifiedTier)
            }
            if (action.switch != null) return null
            return K18n_Tierlist.CantPickTier(specifiedTier)
        }
        val currentPoints = getPointsOfUser(config, data.picks)
        if (officialCost - (officialCost % 2) - (cost - (cost % 2)) > 2) {
            return K18n_Tierlist.GapError(action.officialTier, 1)
        }
        val pointsBack = action.switch?.let { switched -> getPointsForTier(config, switched.tier)!! } ?: 0
        val newPoints = currentPoints - cost + pointsBack
        if (newPoints < 0) {
            return K18n_Tierlist.NotEnoughPoints("$currentPoints - $cost${if (pointsBack == 0) "" else " + $pointsBack"} = $newPoints < 0")
        }
        if (action.switch == null && !canFinishTeamAfterPick(config, options, cost, newPoints)) {
            return K18n_Tierlist.TeamNotCompletable
        }
        context?.saveTier = cost.toString()
        return null
    }

    override suspend fun buildAnnounceData(
        config: TierlistConfig.TierAndPoint,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        return getPossibleTiers(config, picks).entries.filterNot { it.value == 0 }
            .sortedBy { config.tierOrder.indexOf(it.key) }
            .joinToString { tierAmountToString(it.key, it.value) }.let {
                if (it.isEmpty()) null else K18n_League.PossibleTiers(it)
            }?.let { tierMsg ->
                b {
                    "${tierMsg()}, ${K18n_League.PossiblePoints(getPointsOfUser(config, picks))()}"
                }
            }
    }

    override fun getTiers(config: TierlistConfig.TierAndPoint) = config.tierOrder

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistConfig.TierAndPoint,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        val map = getPossibleTiers(config, data.picks).toMutableMap()
        currentState.forEach {
            map.add(it.g.tier.pointsToActualTier(config), -1)
            it.y?.let { y -> map.add(y.tier.pointsToActualTier(config), 1) }
        }
        val result = map.entries.firstOrNull { it.value < 0 }
        val isIllegalFromTiers = result != null
        if (isIllegalFromTiers) {
            return K18n_QueuePicks.LegalTooManyInSingleTier(result.key)
        }
        val cpoints =
            getPointsOfUser(config, data.picks) - currentState.sumOf {
                getPointsForTier(
                    config,
                    it.g.tier
                )!!
            } + currentState.sumOf {
                it.y?.let { y -> getPointsForTier(config, y.tier)!! } ?: 0
            }
        if (cpoints < 0 || !canFinishWithOptions(config, cpoints, map)) {
            return K18n_QueuePicks.LegalTeamCompletion
        }
        return null
    }

    override fun getSingleMap(config: TierlistConfig.TierAndPoint) = config.tiers.mapValues { it.value.amount }

    override fun getCurrentAvailableTiers(config: TierlistConfig.TierAndPoint, picks: List<DraftPokemon>) =
        getPossibleTiers(config, picks).filter { it.value > 0 }.keys.sortedBy { config.tierOrder.indexOf(it) }

    override fun getTierInsertIndex(
        config: TierlistConfig.TierAndPoint,
        picks: List<DraftPokemon>
    ): Int {
        val tier = picks.lastOrNull()?.tier ?: error("No picks to determine tier for index")
        var index = 0
        for (entry in config.tiers.entries.sortedBy { config.tierOrder.indexOf(it.key) }) {
            if (entry.key == tier) {
                return picks.count { !it.free && !it.quit && it.tier == tier } + index - 1
            }
            index += entry.value.amount
        }
        error("Tier $tier not found by")
    }

    override fun getPointsForTier(
        config: TierlistConfig.TierAndPoint,
        tier: String
    ) = tier.toIntOrNull()

    override fun publicTierToDBTier(
        config: TierlistConfig.TierAndPoint,
        tier: String
    ): String {
        return config.tiers[tier]?.tiers?.min()?.toString() ?: error("Tier $tier not found")
    }

    override fun getSortedPicks(
        config: TierlistConfig.TierAndPoint,
        picks: List<DraftPokemon>
    ): List<DraftPokemon> {
        return picks.sortedWith(getTierOrderingComparatorWithoutName(config))
    }

    private fun getPossibleTiers(config: TierlistConfig.TierAndPoint, picks: List<DraftPokemon>) =
        deductPicks(config, picks)

    private fun canFinishTeamAfterPick(
        config: TierlistConfig.TierAndPoint,
        options: Map<String, Int>,
        aboutToPickCost: Int,
        newPoints: Int
    ): Boolean {
        val aboutToPickTier = aboutToPickCost.toString().pointsToActualTier(config)
        val tempOptions = options.toMutableMap()
        tempOptions.add(aboutToPickTier, -1)
        return canFinishWithOptions(config, newPoints, tempOptions)
    }

    private fun canFinishWithOptions(
        config: TierlistConfig.TierAndPoint,
        newPoints: Int,
        tempOptions: Map<String, Int>
    ): Boolean {
        var points = newPoints
        var pointsFromPreviousTier: Int? = null
        for (tier in getTiers(config).reversed()) {
            val pointsFromThisTier = config.tiers[tier]!!.tiers.min()
            tempOptions[tier]?.takeIf { it > 0 }?.let { amount ->
                val actualPoints = pointsFromPreviousTier ?: pointsFromThisTier
                repeat(amount) {
                    points -= actualPoints
                    if (points < 0) return false
                }
            }
            pointsFromPreviousTier = pointsFromThisTier
        }
        return points >= 0
    }

    private fun deductPicks(config: TierlistConfig.TierAndPoint, list: List<DraftPokemon>): Map<String, Int> {
        val map = getSingleMap(config).toMutableMap()
        for (pick in list) {
            pick.takeUnless { it.free || it.quit }?.let { map.add(it.tier.pointsToActualTier(config), -1) }
        }
        return map
    }

    private fun String.pointsToActualTier(config: TierlistConfig.TierAndPoint): String {
        return config.pointsToTier[this] ?: error("No tier found for points $this")
    }
}
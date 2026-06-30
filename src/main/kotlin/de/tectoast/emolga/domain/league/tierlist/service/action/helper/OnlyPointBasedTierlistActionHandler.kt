package de.tectoast.emolga.domain.league.tierlist.service.action.helper

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.OnlyPointBasedTierlistConfig
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import de.tectoast.k18n.generated.K18nMessage

abstract class OnlyPointBasedTierlistActionHandler<C : OnlyPointBasedTierlistConfig> :
    PointBasedTierlistActionHandler<C>(),
    OnlyPointBasedTierlistActionOperations<C> {
    context(data: ValidationRelevantData)
    override fun handleDraftAction(config: C, action: DraftAction, context: DraftActionContext?): ErrorOrNull {
        val cpicks = data.picks
        val currentPoints = getPointsOfUser(config, cpicks)
        val cost = getPointsForTier(config, action.officialTier) ?: return K18n_TierNotFound(action.officialTier)
        val pointsBack = action.switch?.let { switched -> getPointsForTier(config, switched.tier)!! } ?: 0
        val newPoints = currentPoints - cost + pointsBack
        if (newPoints < 0) {
            return K18n_Tierlist.NotEnoughPoints("$currentPoints - $cost${if (pointsBack == 0) "" else " + $pointsBack"} = $newPoints < 0")
        }
        if (action.switch == null) {
            val minimumRequired = minimumNeededPointsForTeamCompletion(config, cpicks.count { !it.noCost } + 1)
            if (newPoints < minimumRequired) {
                return K18n_Tierlist.MinimumNeededError(minimumRequired, newPoints)
            }
        }
        if (action.tera) {
            config.teraMaxPoints?.let {
                if (cpicks.sumOf { dp ->
                        if (dp.tera) getPointsForMon(
                            config,
                            dp
                        ) else 0
                    } + cost - (if (action.switch?.tera == true) pointsBack else 0) > it) {
                    return K18n_Tierlist.TeraMaxPoints(it)
                }
            }
        }
        return null
    }

    context(data: ValidationRelevantData)
    private fun minimumNeededPointsForTeamCompletion(config: C, picksSizeAfter: Int): Int =
        (data.teamSize - picksSizeAfter) * getMinimumPrice(config)

    override suspend fun buildAnnounceData(
        config: C,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        val points = getPointsOfUser(config, picks)
        return K18n_League.PossiblePoints(points)
    }

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: C,
        idx: Int,
        currentState: List<QueuedAction>
    ): ErrorOrNull {
        var gPoints = 0
        var yPoints = 0
        for (data in currentState) {
            gPoints += getPointsForTier(config, data.g.tier)!!
            yPoints += data.y?.let { getPointsForTier(config, it.tier)!! } ?: 0
        }
        if (getPointsOfUser(config, data.picks) - gPoints + yPoints < minimumNeededPointsForTeamCompletion(
                config,
                (data.picks.size) + currentState.size
            )
        ) return K18n_QueuePicks.LegalTeamCompletion
        config.teraMaxPoints?.let {
            val cpicks = data.picks
            val teraPoints = cpicks.sumOf { dp -> if (dp.tera) getPointsForMon(config, dp) else 0 }
            if (teraPoints > it) {
                return K18n_Tierlist.TeraMaxPoints(it)
            }
        }
        return null
    }
}
package de.tectoast.emolga.domain.league.draft.service.random

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierBasedTierlistActionDispatcher
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.features.league.draft.K18n_RandomPick
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single


@Single
class RandomPickTierParseService(
    private val tierBasedDispatcher: TierBasedTierlistActionDispatcher,
    private val normalDispatcher: TierlistActionDispatcher
) {
    fun parseTier(
        tier: String?,
        randomPickConfig: RandomPickConfig,
        tierlistConfig: TierlistConfig,
        picks: List<DraftPokemon>
    ): CalcResult<String> {
        if (tier == null) {
            val chosenTier = if (tierlistConfig is TierBasedTierlistConfig) {
                tierBasedDispatcher.getCurrentAvailableTiers(tierlistConfig, picks).random()
            } else normalDispatcher.getTiers(tierlistConfig).last()
            return chosenTier.success()
        }
        val parsedTier = normalDispatcher.getTiers(tierlistConfig).firstOrNull { it.equals(tier, ignoreCase = true) }
        if (parsedTier == null) {
            return K18n_TierNotFound(tier).error()
        }
        val tierRestrictions = randomPickConfig.tierRestrictions
        if (tierRestrictions.isNotEmpty() && parsedTier !in tierRestrictions) {
            return K18n_RandomPick.OnlyAllowedInTiers(tierRestrictions.joinToString()).error()
        }
        return parsedTier.success()
    }
}

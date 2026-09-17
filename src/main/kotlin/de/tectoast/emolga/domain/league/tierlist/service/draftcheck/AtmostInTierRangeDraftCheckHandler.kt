package de.tectoast.emolga.domain.league.tierlist.service.draftcheck

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.DraftCheck
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import org.koin.core.annotation.Single

@Single
class AtmostInTierRangeDraftCheckHandler : DraftCheckHandler<DraftCheck.AtmostInTierRange> {
    override val targetClass = DraftCheck.AtmostInTierRange::class

    context(data: ValidationRelevantData)
    override suspend fun check(config: DraftCheck.AtmostInTierRange, action: DraftAction): ErrorOrNull {
        if (action.specifiedTier !in config.tiers) return null
        val countInRange = data.picks.count { pick ->
            pick.tier in config.tiers
        }
        if (countInRange >= config.amount) {
            return K18n_Tierlist.AtmostInTierRange(config.amount, config.tiers.joinToString())
        }
        return null
    }
}
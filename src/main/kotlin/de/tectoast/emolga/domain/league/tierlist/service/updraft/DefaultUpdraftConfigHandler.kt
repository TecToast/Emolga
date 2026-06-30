package de.tectoast.emolga.domain.league.tierlist.service.updraft

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.draft.K18n_Tierlist
import org.koin.core.annotation.Single

@Single
class DefaultUpdraftConfigHandler :
    UpdraftConfigHandler<UpdraftConfig.Default> {
    override val targetClass = UpdraftConfig.Default::class

    override fun <T : TierBasedTierlistConfig> handleUpdraft(
        config: UpdraftConfig.Default,
        tierlistConfig: T,
        action: DraftAction,
        tierlistActionHandler: TierBasedTierlistActionHandler<T>
    ): ErrorOrNull {
        val compareResult =
            tierlistActionHandler.compareTiers(tierlistConfig, action.specifiedTier, action.officialTier)
                ?: return K18n_TierNotFound(action.specifiedTier)
        if (action.switch != null) return null
        if (compareResult > 0) {
            return K18n_Tierlist.CantUpdraft(action.officialTier, action.specifiedTier)
        }
        return null
    }
}
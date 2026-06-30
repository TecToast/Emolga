package de.tectoast.emolga.domain.league.tierlist.service.updraft

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
import de.tectoast.emolga.utils.ErrorOrNull
import org.koin.core.annotation.Single

@Single
class NoCheckUpdraftConfigHandler : UpdraftConfigHandler<UpdraftConfig.NoCheck> {
    override val targetClass = UpdraftConfig.NoCheck::class

    override fun <T : TierBasedTierlistConfig> handleUpdraft(
        config: UpdraftConfig.NoCheck,
        tierlistConfig: T,
        action: DraftAction,
        tierlistActionHandler: TierBasedTierlistActionHandler<T>
    ): ErrorOrNull = null
}
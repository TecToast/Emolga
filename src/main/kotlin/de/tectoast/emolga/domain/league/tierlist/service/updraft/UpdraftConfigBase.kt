package de.tectoast.emolga.domain.league.tierlist.service.updraft

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.handler.BaseHandler

interface UpdraftConfigOperations<C : UpdraftConfig> {
    fun <T : TierBasedTierlistConfig> handleUpdraft(
        config: C,
        tierlistConfig: T,
        action: DraftAction,
        tierlistActionHandler: TierBasedTierlistActionHandler<T>
    ): ErrorOrNull
}

interface UpdraftConfigHandler<C : UpdraftConfig> : BaseHandler<C>, UpdraftConfigOperations<C>
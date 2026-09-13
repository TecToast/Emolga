package de.tectoast.emolga.domain.league.tierlist.service.updraft

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.handler.BaseHandler

interface UpdraftConfigOperations<C : UpdraftConfig> {
    fun <T : TierlistConfig> handleUpdraft(
        config: C,
        tierlistConfig: T,
        action: DraftAction,
        tierlistActionHandler: TierlistActionHandler<T>
    ): ErrorOrNull
}

interface UpdraftConfigHandler<C : UpdraftConfig> : BaseHandler<C>, UpdraftConfigOperations<C>
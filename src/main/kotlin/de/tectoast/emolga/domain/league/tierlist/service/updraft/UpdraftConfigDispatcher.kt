package de.tectoast.emolga.domain.league.tierlist.service.updraft

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class UpdraftConfigDispatcher(
    handlers: List<UpdraftConfigHandler<UpdraftConfig>>
) : UpdraftConfigOperations<UpdraftConfig> {
    private val registry = HandlerRegistry(handlers)

    override fun <T : TierlistConfig> handleUpdraft(
        config: UpdraftConfig,
        tierlistConfig: T,
        action: DraftAction,
        tierlistActionHandler: TierlistActionHandler<T>
    ) = registry.getHandler(config).handleUpdraft(config, tierlistConfig, action, tierlistActionHandler)
}

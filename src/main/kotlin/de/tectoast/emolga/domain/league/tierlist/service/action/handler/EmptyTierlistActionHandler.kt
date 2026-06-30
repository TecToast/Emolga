package de.tectoast.emolga.domain.league.tierlist.service.action.handler

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionContext
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import org.koin.core.annotation.Single


@Single(binds = [TierlistActionHandler::class])
class EmptyTierlistActionHandler : TierlistActionHandler<TierlistConfig.Empty>() {
    override val targetClass = TierlistConfig.Empty::class

    context(data: ValidationRelevantData)
    override fun handleDraftAction(
        config: TierlistConfig.Empty,
        action: DraftAction,
        context: DraftActionContext?
    ) = null

    override suspend fun buildAnnounceData(
        config: TierlistConfig.Empty,
        picks: List<DraftPokemon>
    ) = null

    override fun getTiers(config: TierlistConfig.Empty): List<String> = emptyList()

    context(data: ValidationRelevantData)
    override suspend fun checkLegalityOfQueue(
        config: TierlistConfig.Empty,
        idx: Int,
        currentState: List<QueuedAction>
    ) = null
}
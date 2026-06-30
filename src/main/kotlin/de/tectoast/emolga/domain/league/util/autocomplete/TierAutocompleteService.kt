package de.tectoast.emolga.domain.league.util.autocomplete

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.PickContext
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.draft.service.core.DraftRunContextBuilder
import de.tectoast.emolga.domain.league.draft.service.util.DraftCurrentService
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierBasedTierlistActionDispatcher
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.getOrReturn
import org.koin.core.annotation.Single

@Single
class TierAutocompleteService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leaguePickRepo: LeaguePickRepository,
    private val draftCurrentService: DraftCurrentService,
    private val draftRunContextBuilder: DraftRunContextBuilder,
    private val dispatcher: TierBasedTierlistActionDispatcher
) {
    suspend fun autocompleteTier(query: String, channel: Long, user: Long): List<String>? {
        val leagueData = leagueCoreRepo.getDraftRelevantData(channel, locking = false) ?: return null
        val idx =
            draftCurrentService.getCurrentUser(leagueData, user).getOrReturn<PickContext, Unit> { return null }.idx
        val picks = leaguePickRepo.getPicksForUser(leagueData.leagueName, idx)
        val ctx =
            draftRunContextBuilder.build(leagueData, pickContext = PickContext.RegularTurn(idx))
                .getOrReturn<DraftRunContext, Unit> { return null }
        val tierlistConfig = ctx.tierlistMeta.config as? TierBasedTierlistConfig ?: return null
        return dispatcher.getCurrentAvailableTiers(tierlistConfig, picks).filterStartsWithIgnoreCase(query)
    }
}

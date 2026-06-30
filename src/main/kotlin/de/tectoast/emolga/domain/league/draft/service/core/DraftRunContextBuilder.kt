package de.tectoast.emolga.domain.league.draft.service.core

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.model.DraftRelevantLeagueData
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.PickContext
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import org.koin.core.annotation.Single

@Single
class DraftRunContextBuilder(
    private val leagueConfigRepo: LeagueConfigRepository,
    private val tierlistRepository: TierlistRepository
) {
    suspend fun build(
        leagueData: DraftRelevantLeagueData,
        pickContext: PickContext = PickContext.RegularTurn(leagueData.currentIdx),
    ): CalcResult<DraftRunContext> {
        val config = leagueConfigRepo.getConfig(leagueData.leagueName)
        val tierlistMeta =
            tierlistRepository.getMeta(leagueData.guild, config.tlIdentifier) ?: return K18n_NoTierlist.error()
        return DraftRunContext(leagueData, config, tierlistMeta, pickContext).success()
    }
}

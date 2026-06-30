package de.tectoast.emolga.domain.league.doc.service.provider.order

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.doc.model.MonsDocOrderConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierBasedTierlistActionDispatcher
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import org.koin.core.annotation.Single

@Single
class TierSortedMonsDocOrderHandler(
    private val tierlistRepo: TierlistRepository,
    private val tierlistActionDispatcher: TierBasedTierlistActionDispatcher
) : MonsDocOrderHandler<MonsDocOrderConfig.TierSorted> {
    override val targetClass = MonsDocOrderConfig.TierSorted::class

    override suspend fun getDocSortedMons(
        config: MonsDocOrderConfig.TierSorted, guild: Long, leagueConfig: LeagueConfig, picks: List<DraftPokemon>
    ): List<ShowdownID> {
        val tierlistConfig =
            tierlistRepo.getMeta(guild, leagueConfig.tlIdentifier)?.config ?: return picks.map { it.showdownId }
        if (tierlistConfig !is TierBasedTierlistConfig) return picks.map { it.showdownId }
        return tierlistActionDispatcher.getSortedPicks(tierlistConfig, picks).map { it.showdownId }
    }
}
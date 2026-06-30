package de.tectoast.emolga.domain.league.doc.service.provider.order

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.doc.model.MonsDocOrderConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import org.koin.core.annotation.Single

@Single
class PickOrderMonsDocOrderHandler : MonsDocOrderHandler<MonsDocOrderConfig.PickOrder> {
    override val targetClass = MonsDocOrderConfig.PickOrder::class

    override suspend fun getDocSortedMons(
        config: MonsDocOrderConfig.PickOrder, guild: Long, leagueConfig: LeagueConfig, picks: List<DraftPokemon>
    ): List<ShowdownID> {
        return picks.map { it.showdownId }
    }
}
package de.tectoast.emolga.domain.league.doc.service.provider.order

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.doc.model.MonsDocOrderConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.handler.BaseHandler

interface MonsDocOrderOperations<C : MonsDocOrderConfig> {
    suspend fun getDocSortedMons(
        config: C, guild: Long, leagueConfig: LeagueConfig, picks: List<DraftPokemon>
    ): List<ShowdownID>
}

interface MonsDocOrderHandler<C : MonsDocOrderConfig> : BaseHandler<C>, MonsDocOrderOperations<C>
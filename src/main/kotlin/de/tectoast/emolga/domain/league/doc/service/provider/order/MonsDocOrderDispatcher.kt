package de.tectoast.emolga.domain.league.doc.service.provider.order

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.doc.model.MonsDocOrderConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class MonsDocOrderDispatcher(handlers: List<MonsDocOrderHandler<MonsDocOrderConfig>>) :
    MonsDocOrderOperations<MonsDocOrderConfig> {
    private val registry = HandlerRegistry(handlers)

    override suspend fun getDocSortedMons(
        config: MonsDocOrderConfig, guild: Long, leagueConfig: LeagueConfig, picks: List<DraftPokemon>
    ) = registry.getHandler(config).getDocSortedMons(config, guild, leagueConfig, picks)
}
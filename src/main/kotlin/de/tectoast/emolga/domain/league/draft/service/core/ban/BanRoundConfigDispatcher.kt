package de.tectoast.emolga.domain.league.draft.service.core.ban

import de.tectoast.emolga.domain.league.draft.model.ban.BanRoundConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class BanRoundConfigDispatcher(handlers: List<BanRoundConfigHandler<BanRoundConfig>>) :
    BanRoundConfigOperations<BanRoundConfig> {
    private val registry = HandlerRegistry(handlers)

    override fun checkBan(
        config: BanRoundConfig, tier: String, alreadyBanned: Set<DraftPokemon>
    ) = registry.getHandler(config).checkBan(config, tier, alreadyBanned)

    override fun getPossibleBanTiers(
        config: BanRoundConfig, alreadyBanned: Set<DraftPokemon>
    ) = registry.getHandler(config).getPossibleBanTiers(config, alreadyBanned)
}
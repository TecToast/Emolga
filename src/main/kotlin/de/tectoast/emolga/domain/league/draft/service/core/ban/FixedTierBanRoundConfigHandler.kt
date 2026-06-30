package de.tectoast.emolga.domain.league.draft.service.core.ban

import de.tectoast.emolga.domain.league.draft.model.ban.BanRoundConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.league.K18n_BanRoundConfig
import org.koin.core.annotation.Single

@Single
class FixedTierBanRoundConfigHandler : BanRoundConfigHandler<BanRoundConfig.FixedTier> {
    override val targetClass = BanRoundConfig.FixedTier::class

    override fun checkBan(
        config: BanRoundConfig.FixedTier, tier: String, alreadyBanned: Set<DraftPokemon>
    ) = if (config.tier != tier) K18n_BanRoundConfig.FixedTierError(config.tier) else null

    override fun getPossibleBanTiers(
        config: BanRoundConfig.FixedTier, alreadyBanned: Set<DraftPokemon>
    ) = listOf(config.tier)
}
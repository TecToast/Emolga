package de.tectoast.emolga.domain.league.draft.service.core.ban

import de.tectoast.emolga.domain.league.draft.model.ban.BanRoundConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.league.K18n_BanRoundConfig
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class FixedTierSetBanRoundConfigHandler : BanRoundConfigHandler<BanRoundConfig.FixedTierSet> {
    override val targetClass = BanRoundConfig.FixedTierSet::class

    override fun checkBan(
        config: BanRoundConfig.FixedTierSet, tier: String, alreadyBanned: Set<DraftPokemon>
    ): K18nMessage? {
        val originalBanAmount = config.tierSet[tier] ?: return K18n_BanRoundConfig.FixedTierSetTierNotBannable(tier)
        val alreadyBannedAmount = alreadyBanned.count { it.tier == tier }
        return if (originalBanAmount - alreadyBannedAmount <= 0) K18n_BanRoundConfig.FixedTierSetCantBanFromThatTier(
            tier
        ) else null
    }

    override fun getPossibleBanTiers(
        config: BanRoundConfig.FixedTierSet, alreadyBanned: Set<DraftPokemon>
    ): List<String> {
        val alreadyBanned = alreadyBanned.groupBy { it.tier }
        return config.tierSet.entries.filter { it.value - (alreadyBanned[it.key]?.size ?: 0) > 0 }.map { it.key }
    }
}
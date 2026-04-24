package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.BaseHandler
import de.tectoast.emolga.database.exposed.HandlerRegistry
import de.tectoast.emolga.league.K18n_BanRoundConfig
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

interface BanRoundConfigOperations<C : BanRoundConfig> {
    fun checkBan(config: C, tier: String, alreadyBanned: Set<DraftPokemon>): K18nMessage?
    fun getPossibleBanTiers(config: C, alreadyBanned: Set<DraftPokemon>): List<String>
}

interface BanRoundConfigHandler<C : BanRoundConfig> : BaseHandler<C>, BanRoundConfigOperations<C>

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

@Serializable
sealed interface BanRoundConfig {

    @Serializable
    @SerialName("FixedTier")
    data class FixedTier(val tier: String) : BanRoundConfig

    @Serializable
    @SerialName("FixedTierSet")
    data class FixedTierSet(val tierSet: Map<String, Int>) : BanRoundConfig
}


enum class BanSkipBehavior {
    NOTHING, RANDOM
}

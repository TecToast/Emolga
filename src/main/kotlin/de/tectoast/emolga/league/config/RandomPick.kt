package de.tectoast.emolga.league.config

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.ArgumentPresence
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.isMega
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.toSDName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging

@Serializable
data class RandomPickConfig(
    val enabled: Boolean = true,
    val mode: RandomPickMode = RandomPickMode.Default(),
    val jokers: Int = 0,
    val onlyOneMega: Boolean = false,
    val tierRestrictions: Set<String> = emptySet()
) {
    fun hasJokers() = jokers > 0
}

data class RandomPickUserInput(
    val tier: String?,
    val type: String?,
    val ignoreRestrictions: Boolean = false,
    val skipMons: Set<String> = emptySet()
)

@Serializable
sealed interface RandomPickMode {
    context(iData: InteractionData)
    suspend fun League.getRandomPick(input: RandomPickUserInput, config: RandomPickConfig): Pair<DraftName, String>?

    /**
     * @return a map of the possible command options for the randompick command [true = required, false = optional, null = not available]
     */
    fun provideCommandOptions(): Map<RandomPickArgument, ArgumentPresence>

    @Serializable
    @SerialName("Default")
    data class Default(val tierRequired: Boolean = false, val typeAllowed: Boolean = true) : RandomPickMode {
        override fun provideCommandOptions(): Map<RandomPickArgument, ArgumentPresence> {
            return buildMap {
                put(RandomPickArgument.TIER, if (tierRequired) ArgumentPresence.REQUIRED else ArgumentPresence.OPTIONAL)
                if (typeAllowed) put(RandomPickArgument.TYPE, ArgumentPresence.OPTIONAL)
            }
        }

        context(iData: InteractionData)
        override suspend fun League.getRandomPick(
            input: RandomPickUserInput, config: RandomPickConfig
        ): Pair<DraftName, String>? {
            if (tierRequired && input.tier == null) return iData.replyNull("Du musst ein Tier angeben!")
            val tier = if (input.ignoreRestrictions) input.tier!! else parseTier(input.tier, config) ?: return null
            val list = tierlist.getByTier(tier)!!.shuffled()
            val skipMega = config.onlyOneMega && picks[current]!!.any { it.name.isMega }
            return firstAvailableMon(list) { german, english ->
                if (german in input.skipMons || (skipMega && english.isMega)) return@firstAvailableMon false
                if (input.type != null) input.type in db.pokedex.get(english.toSDName())!!.types else true
            }?.let { it to tier }
                ?: return iData.replyNull("In diesem Tier gibt es kein Pokemon mit dem angegebenen Typen mehr!")
        }
    }

    // Only compatible with TierlistMode.TIERS
    @Serializable
    @SerialName("TypeTierlist")
    data object TypeTierlist : RandomPickMode {
        private val logger = KotlinLogging.logger {}
        override fun provideCommandOptions(): Map<RandomPickArgument, ArgumentPresence> {
            return mapOf(RandomPickArgument.TYPE to ArgumentPresence.REQUIRED)
        }

        context(iData: InteractionData)
        override suspend fun League.getRandomPick(
            input: RandomPickUserInput, config: RandomPickConfig
        ): Pair<DraftName, String>? {
            val type = input.type ?: return iData.replyNull("Du musst einen Typen angeben!")
            val picks = picks[current]!!
            var mon: DraftName? = null
            var tier: String? = null
            val usedTiers = mutableSetOf<String>()
            val skipMega = config.onlyOneMega && picks.any { it.name.isMega }
            val prices = tierlist.withTierBasedPriceManager { it.getSingleMap() }
                ?: return iData.replyNull("Die Tierlist unterstützt kein Randompick mit TypeTierlist-Modus!")
            run {
                repeat(prices.size) {
                    val temptier =
                        prices.filter { (tier, amount) -> tier !in usedTiers && picks.count { mon -> mon.tier == tier } < amount }.keys.randomOrNull()
                            ?: return iData.replyNull("Es gibt kein $type-Pokemon mehr, welches in deinen Kader passt!")
                    val tempmon = firstAvailableMon(
                        tierlist.getWithTierAndType(temptier, type).shuffled()
                    ) { german, _ -> german in input.skipMons && !(german.isMega && skipMega) }
                    if (tempmon != null) {
                        mon = tempmon
                        tier = temptier
                        return@run
                    }
                    usedTiers += temptier
                }
            }
            if (mon == null) {
                logger.error("No pokemon found without error message: $current $type")
                return iData.replyNull("Es ist konnte kein passendes Pokemon gefunden werden! (<@${Constants.FLOID}>)")
            }
            return mon to tier!!
        }
    }

    context(league: League, iData: InteractionData)
    fun parseTier(tier: String?, config: RandomPickConfig): String? {
        val tl = league.tierlist
        if (tier == null) return tl.withTierBasedPriceManager { it.getCurrentAvailableTiers().random() }
            ?: tl.withTL { it.getTiers().last() }
        val parsedTier = league.tierlist.order.firstOrNull { it.equals(tier, ignoreCase = true) }
        if (parsedTier == null) {
            return iData.replyNull("Das Tier `$tier` existiert nicht!")
        }
        if (config.tierRestrictions.isNotEmpty() && parsedTier !in config.tierRestrictions) {
            return iData.replyNull("In dieser Liga darf nur in folgenden Tiers gerandompickt werden: ${config.tierRestrictions.joinToString()}")
        }
        return parsedTier
    }
}

@Serializable
data class RandomLeagueData(
    var currentMon: RandomLeaguePick? = null, val usedJokers: MutableMap<Int, Int> = mutableMapOf()
)

@Serializable
data class RandomLeaguePick(
    val official: String,
    val tlName: String,
    val tier: String,
    val data: Map<String, String?> = mapOf(),
    val history: Set<String> = setOf(),
    var disabled: Boolean = false
)


enum class RandomPickArgument {
    TIER, TYPE
}
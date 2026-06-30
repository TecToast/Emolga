package de.tectoast.emolga.domain.league.tierlist.service.action.handler

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.CombinedOptionsTierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.addFromMutable
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.subtractFrom
import de.tectoast.generic.K18n_Or
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single(binds = [TierlistActionHandler::class, TierBasedTierlistActionHandler::class])
class OptionsTierBasedTierlistActionHandler :
    CombinedOptionsTierlistActionHandler<TierlistConfig.OptionsTierBased>() {
    override val targetClass = TierlistConfig.OptionsTierBased::class

    override suspend fun buildAnnounceData(
        config: TierlistConfig.OptionsTierBased,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        val res = getAllPossibleTiers(config, picks)
        val allTiers = res.flatMapTo(mutableSetOf()) { it.keys }.sortedBy {
            config.tierOrder.indexOf(it)
        }
        val minValues = allTiers.associateWith { tier ->
            res.minOf { it[tier] ?: 0 }
        }
        if (res.all { map -> map.all { it.value <= 0 } }) {
            return null
        }
        return b {
            val str = buildString {
                val baseData = allTiers.filter { minValues[it]!! > 0 }.joinToString(", ") {
                    tierAmountToString(it, minValues[it]!!)
                }
                val additionalData = res.mapNotNull { map ->
                    val reduced = map.subtractFrom(minValues)
                    if (reduced.all { it.value <= 0 }) null else allTiers.filter { reduced[it]!! > 0 }
                        .joinToString(", ") {
                            tierAmountToString(it, reduced[it]!!)
                        }
                }.joinToString(" **--- ${K18n_Or()} ---** ")
                append(baseData)
                if (additionalData.isNotEmpty()) {
                    append(" + [")
                    append(additionalData)
                    append("]")
                }
            }
            K18n_League.PossibleTiers(str)()
        }
    }

    override fun getSingleMap(config: TierlistConfig.OptionsTierBased): Map<String, Int> {
        return config.genericTiers.toMutableMap().apply {
            config.options.forEach { optionList ->
                this.addFromMutable(optionList.firstOrNull().orEmpty())
            }
        }
    }

    override fun getTierInsertIndex(
        config: TierlistConfig.OptionsTierBased,
        picks: List<DraftPokemon>
    ): Int {
        error("Can't get tier insert index for option based tierlist")
    }
}
package de.tectoast.emolga.domain.league.tierlist.service.action.handler

import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.tierlist.model.config.TierlistConfig
import de.tectoast.emolga.domain.league.tierlist.service.action.TierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.CombinedOptionsTierlistActionHandler
import de.tectoast.emolga.domain.league.tierlist.service.action.helper.TierBasedTierlistActionHandler
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.add
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single(binds = [TierlistActionHandler::class, TierBasedTierlistActionHandler::class])
class ChoiceTierBasedTierlistActionHandler :
    CombinedOptionsTierlistActionHandler<TierlistConfig.ChoiceTierBased>() {
    override val targetClass = TierlistConfig.ChoiceTierBased::class

    override suspend fun buildAnnounceData(
        config: TierlistConfig.ChoiceTierBased,
        picks: List<DraftPokemon>
    ): K18nMessage? {
        val fromGeneric = config.genericTiers.deductPicks(picks)
        val singularOptions = getSingularChoiceList(config)
        for (tier in fromGeneric.flatMap { genericEntry ->
            if (genericEntry.value >= 0) emptyList()
            else List(-genericEntry.value) { genericEntry.key }
        }) {
            val result = singularOptions.removeOne { tier in it.tiers }
            if (result == null) error("Couldn't find tier $tier in choices")
        }
        val availableOptions = singularOptions.groupingBy { it.tiers }.eachCount().toMutableMap()
        val str = buildString {
            val baseData =
                fromGeneric.entries.filter { it.value > 0 }.sortedBy { config.tierOrder.indexOf(it.key) }.flatMap {
                    buildList {
                        add(tierAmountToString(it.key, it.value))
                        availableOptions.entries.firstOrNull { en -> it.key in en.key }?.let { entry ->
                            availableOptions.remove(entry.key)
                            add(tierAmountToString(entry.key.sortedBy { k -> config.tierOrder.indexOf(k) }
                                .joinToString("/"), entry.value))
                        }
                    }
                }.joinToString(", ")
            val additionalData = availableOptions.entries.joinToString { (tiers, amount) ->
                tierAmountToString(tiers.sortedBy { config.tierOrder.indexOf(it) }.joinToString("/"), amount)
            }
            append(baseData)
            if (additionalData.isNotEmpty()) {
                append(", ")
                append(additionalData)
            }
        }
        return if (str.isEmpty()) null else K18n_League.PossibleTiers(str)
    }

    override fun getSingleMap(config: TierlistConfig.ChoiceTierBased): Map<String, Int> {
        return config.genericTiers.toMutableMap().apply {
            for (choice in config.choices) {
                val firstOption = choice.tiers.first()
                this.add(firstOption, choice.amount)
            }
        }
    }

    override fun getTierInsertIndex(
        config: TierlistConfig.ChoiceTierBased,
        picks: List<DraftPokemon>
    ): Int {
        val tierToInsert = picks.lastOrNull()?.tier ?: error("No tier found in picks to use")
        var index = 0
        var tierBefore: String? = null
        for (entry in config.genericTiers.entries.sortedBy { config.tierOrder.indexOf(it.key) }) {
            val sumOfChoiceSlots =
                config.choices.filter { entry.key in it.tiers && (tierBefore == null || tierBefore in it.tiers) }
                    .sumOf { it.amount }
            index += sumOfChoiceSlots
            if (entry.key == tierToInsert) {
                val picksAmountInTier = picks.count { !it.free && !it.quit && it.tier == tierToInsert }
                val monsInChoiceSlots = picksAmountInTier - entry.value
                return if (monsInChoiceSlots > 0) {
                    if (sumOfChoiceSlots > 0) index - monsInChoiceSlots else index + monsInChoiceSlots
                } else picksAmountInTier + index - 1
            }
            index += entry.value
            tierBefore = entry.key
        }
        error("Tier $tierToInsert not found")
    }

    private fun getSingularChoiceList(config: TierlistConfig.ChoiceTierBased) =
        TierlistConfig.ChoiceTierBased.ChoiceTierOption.createSingularList(config.choices)

    private inline fun <T> MutableCollection<T>.removeOne(predicate: (T) -> Boolean): T? {
        val item = this.firstOrNull(predicate) ?: return null
        this.remove(item)
        return item
    }
}
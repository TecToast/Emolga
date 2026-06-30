package de.tectoast.emolga.domain.league.tierlist.model.config

import de.tectoast.emolga.domain.league.tierlist.model.DraftCheck
import de.tectoast.emolga.domain.league.tierlist.model.UpdraftConfig
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.addFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TierlistConfig {
    val draftChecks: List<DraftCheck>


    @Serializable
    @SerialName("SimpleTierBased")
    data class SimpleTierBased(
        val tierOrder: List<String>,
        val tiers: Map<String, Int>,
        override val updraftConfig: UpdraftConfig = UpdraftConfig.Default,
        override val draftChecks: List<DraftCheck> = emptyList()
    ) : TierlistConfig, TierBasedTierlistConfig


    @Serializable
    @SerialName("SimplePointBased")
    data class SimplePointBased(
        val prices: Map<String, Int>,
        override val globalPoints: Int,
        override val draftChecks: List<DraftCheck> = emptyList(),
        override val teraMaxPoints: Int? = null
    ) : TierlistConfig, OnlyPointBasedTierlistConfig

    @Serializable
    @SerialName("RangePointBased")
    data class RangePointBased(
        val maxTier: Int,
        val minTier: Int,
        override val globalPoints: Int,
        override val draftChecks: List<DraftCheck> = emptyList(),
        override val teraMaxPoints: Int? = null
    ) : TierlistConfig, OnlyPointBasedTierlistConfig

    @Serializable
    @SerialName("OptionsTierBased")
    data class OptionsTierBased(
        override val tierOrder: List<String>,
        val genericTiers: Map<String, Int>,
        val options: List<List<Map<String, Int>>>,
        override val updraftConfig: UpdraftConfig = UpdraftConfig.Default,
        override val draftChecks: List<DraftCheck> = emptyList()
    ) : TierlistConfig, CombinedOptionsTierlistConfig {
        override val combinedOptions by lazy {
            buildList {
                for (set in options) {
                    for (option in set) {
                        add(genericTiers.addFrom(option))
                    }
                }
            }
        }
    }

    @Serializable
    @SerialName("ChoiceTierBased")
    data class ChoiceTierBased(
        override val tierOrder: List<String>,
        val genericTiers: Map<String, Int>,
        val choices: List<ChoiceTierOption>,
        override val updraftConfig: UpdraftConfig = UpdraftConfig.Default,
        override val draftChecks: List<DraftCheck> = emptyList()
    ) : TierlistConfig, CombinedOptionsTierlistConfig {
        override val combinedOptions: List<Map<String, Int>> by lazy {
            buildList {
                fun recursiveBuild(remainingChoices: List<SingularChoiceTierOption>, map: Map<String, Int>) {
                    if (remainingChoices.isEmpty()) {
                        add(map)
                        return
                    }
                    val first = remainingChoices.first()
                    val rest = remainingChoices.drop(1)
                    for (tier in first.tiers) {
                        val copy = map.toMutableMap()
                        copy.add(tier, 1)
                        recursiveBuild(rest, copy)
                    }
                }
                recursiveBuild(ChoiceTierOption.createSingularList(choices), genericTiers)
            }.distinct()
        }


        @Serializable
        data class ChoiceTierOption(
            val tiers: Set<String>, val amount: Int
        ) {
            companion object {
                fun createSingularList(list: List<ChoiceTierOption>) = list.flatMapTo(mutableListOf()) { option ->
                    List(option.amount) {
                        SingularChoiceTierOption(option.tiers)
                    }
                }
            }
        }

        data class SingularChoiceTierOption(
            val tiers: Set<String>
        )

    }

    @Serializable
    @SerialName("FreePick")
    data class FreePick(
        override val draftChecks: List<DraftCheck> = emptyList(),
        override val globalPoints: Int,
        override val updraftConfig: UpdraftConfig = UpdraftConfig.Default,
        val tierOrder: List<String>,
        val normalTiers: Map<String, Int>,
        val pointPrices: Map<String, Int>,
        val freeAmount: Int
    ) : TierlistConfig, FreePickTierlistConfig {
        override val teraMaxPoints = null
    }

    @Serializable
    @SerialName("TierAndPoint")
    data class TierAndPoint(
        override val updraftConfig: UpdraftConfig = UpdraftConfig.NoCheck,
        override val draftChecks: List<DraftCheck> = emptyList(),
        override val globalPoints: Int,
        override val teraMaxPoints: Int? = null,
        val tiers: Map<String, SingleTierAndPointData>,
        val tierOrder: List<String>
    ) : TierlistConfig, TierBasedTierlistConfig, PointBasedTierlistConfig {
        @Serializable
        data class SingleTierAndPointData(
            val amount: Int, val tiers: List<Int>
        )

        val pointsToTier by lazy {
            tiers.entries.flatMap { it.value.tiers.map { num -> num.toString() to it.key } }.toMap()
        }

    }

    @Serializable
    @SerialName("Empty")
    data object Empty : TierlistConfig {
        override val draftChecks: List<DraftCheck> = emptyList()
    }
}

package de.tectoast.emolga.domain.league.tierlist.model.config

interface CombinedOptionsTierlistConfig : TierBasedTierlistConfig {
    val combinedOptions: List<Map<String, Int>>
    val tierOrder: List<String>
}
package de.tectoast.emolga.domain.league.tierlist.model.config

interface PointBasedTierlistConfig : TierlistConfig {
    val globalPoints: Int
    val teraMaxPoints: Int?
}
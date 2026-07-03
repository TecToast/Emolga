package de.tectoast.emolga.domain.league.config.model

import kotlinx.serialization.Serializable

@Serializable
data class Triggers(
    val queuePicks: Boolean = true,
    val allowPickDuringSwitch: Boolean = false,
    val alwaysShowTier: Boolean = false,
    val bo3: Boolean = false,
    val teamSubmit: Boolean = false,
    val randomBattle: Boolean = false,
    val replaceOnSwitch: Boolean = false,
) {
    operator fun plus(other: TriggersOverride?): Triggers {
        if (other == null) return this
        return Triggers(
            queuePicks = other.queuePicks ?: queuePicks,
            allowPickDuringSwitch = other.allowPickDuringSwitch ?: allowPickDuringSwitch,
            alwaysShowTier = other.alwaysSendTierOnPick ?: alwaysShowTier,
            bo3 = other.bo3 ?: bo3,
            teamSubmit = other.teamSubmit ?: teamSubmit,
            randomBattle = other.randomBattle ?: randomBattle,
            replaceOnSwitch = other.replaceOnSwitch ?: replaceOnSwitch
        )
    }
}

@Serializable
data class TriggersOverride(
    val queuePicks: Boolean? = null,
    val allowPickDuringSwitch: Boolean? = null,
    val alwaysSendTierOnPick: Boolean? = null,
    val bo3: Boolean? = null,
    val teamSubmit: Boolean? = null,
    val randomBattle: Boolean? = null,
    val replaceOnSwitch: Boolean? = null
)

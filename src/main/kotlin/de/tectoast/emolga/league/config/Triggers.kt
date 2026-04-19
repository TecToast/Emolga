package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

data class Triggers(
    val queuePicks: Boolean = true,
    val allowPickDuringSwitch: Boolean = false,
    val alwaysSendTierOnPick: Boolean = false,
    val saveReplayData: Boolean = false,
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
            alwaysSendTierOnPick = other.alwaysSendTierOnPick ?: alwaysSendTierOnPick,
            saveReplayData = other.saveReplayData ?: saveReplayData,
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
    val saveReplayData: Boolean? = null,
    val bo3: Boolean? = null,
    val teamSubmit: Boolean? = null,
    val randomBattle: Boolean? = null,
    val replaceOnSwitch: Boolean? = null
)
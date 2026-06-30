package de.tectoast.emolga.domain.league.draft.model.random

import kotlinx.serialization.Serializable

data class RandomPickConfig(
    val enabled: Boolean = true,
    val mode: RandomPickMode = RandomPickMode.Default(),
    val jokers: Int = 0,
    val tierRestrictions: Set<String> = emptySet()
) {
    fun hasJokers() = jokers > 0

    operator fun plus(other: RandomPickConfigOverride?): RandomPickConfig {
        if (other == null) return this
        return RandomPickConfig(
            enabled = other.enabled ?: enabled,
            mode = other.mode ?: mode,
            jokers = other.jokers ?: jokers,
            tierRestrictions = other.tierRestrictions ?: tierRestrictions
        )
    }
}

@Serializable
data class RandomPickConfigOverride(
    val enabled: Boolean? = null,
    val mode: RandomPickMode? = null,
    val jokers: Int? = null,
    val tierRestrictions: Set<String>? = null
)
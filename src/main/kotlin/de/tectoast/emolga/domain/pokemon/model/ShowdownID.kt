package de.tectoast.emolga.domain.pokemon.model

import de.tectoast.emolga.utils.serializer.ShowdownIDSerializer
import kotlinx.serialization.Serializable

/**
 * A wrapper for a Showdown ID (mostly Pokémon). This is used to distinguish between normal strings and showdown IDs, which have specific formatting rules and are used in multiple places in the codebase. By using a value class, we can ensure type safety and avoid confusion between different string types.
 */
@JvmInline
@Serializable(with = ShowdownIDSerializer::class)
value class ShowdownID(val value: String) : Comparable<ShowdownID> {
    override fun compareTo(other: ShowdownID): Int = value.compareTo(other.value)

    override fun toString(): String {
        return value
    }
}

package de.tectoast.emolga.domain.league.queue.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class QueuedMon(
    val id: ShowdownID,
    val tier: String,
    val free: Boolean = false,
    val tera: Boolean = false,
    @SerialName("ts")
    val tierSpecified: Boolean = false
)

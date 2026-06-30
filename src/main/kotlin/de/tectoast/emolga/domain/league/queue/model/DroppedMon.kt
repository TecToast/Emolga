package de.tectoast.emolga.domain.league.queue.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class DroppedMon(val id: ShowdownID, val tier: String)
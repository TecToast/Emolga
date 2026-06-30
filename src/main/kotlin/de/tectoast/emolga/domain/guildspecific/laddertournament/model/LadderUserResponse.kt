package de.tectoast.emolga.domain.guildspecific.laddertournament.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import kotlinx.serialization.Serializable

@Serializable
data class LadderUserResponse(
    val username: String, val ratings: Map<ShowdownID, LadderRankData>
)
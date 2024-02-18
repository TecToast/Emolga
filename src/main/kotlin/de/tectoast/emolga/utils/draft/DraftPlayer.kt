package de.tectoast.emolga.utils.draft

import kotlinx.serialization.Serializable

@Serializable
data class DraftPlayer(
    var alivePokemon: Int, var winner: Boolean
)

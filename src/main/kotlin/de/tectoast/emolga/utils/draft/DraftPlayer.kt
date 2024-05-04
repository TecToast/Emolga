package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.utils.showdown.SDPlayer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class DraftPlayer(
    var alivePokemon: Int, var winner: Boolean, @Transient val sdPlayer: SDPlayer? = null
)

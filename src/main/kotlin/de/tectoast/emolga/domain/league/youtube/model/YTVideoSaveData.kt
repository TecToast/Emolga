package de.tectoast.emolga.domain.league.youtube.model

import kotlinx.serialization.Serializable

@Serializable
data class YTVideoSaveData(
    val enabled: Boolean, val vids: Map<Int, String>
)
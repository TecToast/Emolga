package de.tectoast.emolga.league.config

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeConfig(
    val sendChannel: Long,
)
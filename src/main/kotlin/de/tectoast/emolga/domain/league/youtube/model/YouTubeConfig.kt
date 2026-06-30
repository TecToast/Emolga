package de.tectoast.emolga.domain.league.youtube.model

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeConfig(
    val sendChannel: Long,
    val messageConfig: YouTubeMessageConfig = YouTubeMessageConfig(),
)
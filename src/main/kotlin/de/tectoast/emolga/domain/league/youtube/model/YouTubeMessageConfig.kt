package de.tectoast.emolga.domain.league.youtube.model

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeMessageConfig(
    val includeRolePing: Long? = null,
)

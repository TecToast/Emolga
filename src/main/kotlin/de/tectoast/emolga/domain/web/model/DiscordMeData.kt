package de.tectoast.emolga.domain.web.model

import kotlinx.serialization.Serializable

@Serializable
data class DiscordMeData(
    val userId: String,
    val displayName: String,
    val avatar: String
)
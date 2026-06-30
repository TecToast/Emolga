package de.tectoast.emolga.domain.league.signup.model.data

import kotlinx.serialization.Serializable

@Serializable
data class ParticipantData(
    val id: Int,
    val users: List<UserData>,
    val data: Map<String, String>,
    val hasLogo: Boolean,
    val conference: String? = null,
)
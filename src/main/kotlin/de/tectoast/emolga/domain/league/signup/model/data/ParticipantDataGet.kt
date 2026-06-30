package de.tectoast.emolga.domain.league.signup.model.data

import kotlinx.serialization.Serializable


@Serializable
data class ParticipantDataGet(val conferences: List<String>, val data: List<ParticipantData>)

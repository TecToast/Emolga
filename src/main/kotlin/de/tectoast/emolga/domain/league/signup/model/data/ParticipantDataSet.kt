package de.tectoast.emolga.domain.league.signup.model.data

import kotlinx.serialization.Serializable

@Serializable
data class ParticipantDataSet(val conferences: List<String>, val data: Map<Int, String?>)
package de.tectoast.emolga.domain.league.signup.model.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(val id: String, val name: String, val avatar: String)
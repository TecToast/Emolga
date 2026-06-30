package de.tectoast.emolga.domain.league.result.model

import kotlinx.serialization.Serializable

@Serializable
data class ResultUserData(val name: String, val avatarUrl: String, val picks: List<ResultCodePokemon>)
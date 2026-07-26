package de.tectoast.emolga.domain.league.result.model

import kotlinx.serialization.Serializable

@Serializable
data class ResultCodeResponse(
    val guildName: String, val logoUrl: String?, val bo3: Boolean, val week: Int, val data: List<ResultUserData>
)
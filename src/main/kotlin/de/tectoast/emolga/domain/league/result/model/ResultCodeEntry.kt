package de.tectoast.emolga.domain.league.result.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid


@Serializable
data class ResultCodeEntry(
    val code: Uuid,
    val leagueName: String,
    val guild: Long,
    val week: Int,
    val p1: Int,
    val p2: Int
)
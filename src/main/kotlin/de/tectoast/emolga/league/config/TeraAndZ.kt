package de.tectoast.emolga.league.config

import de.tectoast.emolga.leaguecreator.DynamicCoord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TeraAndZ")
data class TeraAndZ(val z: TZDataHolder? = null, val tera: TeraData? = null)

@Serializable
data class TZDataHolder(
    val coord: DynamicCoord, val searchRange: String, val searchColumn: Int, val firstTierAllowed: String? = null
)

@Serializable
data class TeraData(val type: TZDataHolder, val mon: TZDataHolder)
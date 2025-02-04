package de.tectoast.emolga.league.config

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable


@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TimerRelated(
    var cooldown: Long = -1,
    var regularCooldown: Long = -1,
    @EncodeDefault var lastPick: Long = -1,
    var lastRegularDelay: Long = -1,
    @EncodeDefault val usedStallSeconds: MutableMap<Int, Int> = mutableMapOf(),
    var lastStallSecondUsedMid: Long? = null
)
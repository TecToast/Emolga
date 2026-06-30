package de.tectoast.emolga.domain.league.draft.model.timer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TimerRelated(
    var cooldown: Instant = Instant.DISTANT_PAST,
    var regularCooldown: Instant = Instant.DISTANT_PAST,
    var lastPick: Instant = Instant.DISTANT_PAST,
    var lastRegularDelay: Duration = (-1).seconds,
    val usedStallSeconds: MutableMap<Int, Int> = mutableMapOf(),
    var lastStallSecondUsedMid: Long? = null
) {
    fun stallSecondsActive() = cooldown != regularCooldown
}

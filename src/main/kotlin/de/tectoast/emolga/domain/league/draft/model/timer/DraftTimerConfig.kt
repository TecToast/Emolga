package de.tectoast.emolga.domain.league.draft.model.timer

import de.tectoast.emolga.utils.serializer.TreeMapSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.time.Instant

@Serializable
sealed class DraftTimerConfig {
    var timerStart: Instant? = null
    var stallSeconds: Int = 0
    var oneTimerForAllPicks: Boolean = false
    var startPunishSkipsTime: Instant = Instant.DISTANT_PAST

    @Serializable
    @SerialName("ClockDependent")
    class ClockDependentTimer(val timers: @Serializable(with = TreeMapSerializer::class) TreeMap<Long, TimerInfo>) :
        DraftTimerConfig()

    @Serializable
    @SerialName("Simple")
    class SimpleTimer(val timerInfo: TimerInfo) : DraftTimerConfig()

    @Serializable
    @SerialName("Switch")
    class SwitchTimer(val timerInfos: Map<String, TimerInfo>, var currentTimer: String = timerInfos.keys.first()) :
        DraftTimerConfig()
}
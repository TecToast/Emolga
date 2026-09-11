package de.tectoast.emolga.domain.league.draft.model.timer

import de.tectoast.emolga.utils.serializer.TreeMapSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
sealed interface DraftTimerConfig {
    val generalConfig: GeneralDraftTimerConfig

    @Serializable
    @SerialName("ClockDependent")
    class ClockDependentTimer(
        override val generalConfig: GeneralDraftTimerConfig = GeneralDraftTimerConfig(),
        val timers: @Serializable(with = TreeMapSerializer::class) TreeMap<Long, TimerInfo>
    ) :
        DraftTimerConfig

    @Serializable
    @SerialName("Simple")
    class SimpleTimer(
        override val generalConfig: GeneralDraftTimerConfig = GeneralDraftTimerConfig(),
        val timerInfo: TimerInfo
    ) : DraftTimerConfig

    @Serializable
    @SerialName("Switch")
    class SwitchTimer(
        override val generalConfig: GeneralDraftTimerConfig = GeneralDraftTimerConfig(),
        val timerInfos: Map<String, TimerInfo>,
        var currentTimer: String = timerInfos.keys.first()
    ) :
        DraftTimerConfig
}
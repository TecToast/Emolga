package de.tectoast.emolga.domain.league.draft.model.timer

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GeneralDraftTimerConfig(
    val timerStart: Instant? = null,
    val timerAfterRound: Int? = null,
    var stallSeconds: Int = 0,
    val oneTimerForAllPicks: Boolean = false,
    val startPunishSkipsTime: Instant = Instant.DISTANT_PAST,
)
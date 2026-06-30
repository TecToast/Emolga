package de.tectoast.emolga.domain.league.draft.model.execution

import de.tectoast.emolga.domain.league.draft.util.DisplayHelper
import de.tectoast.k18n.generated.K18nMessage


data class TimerSkipData(
    val result: TimerSkipResult,
    val message: (suspend (DisplayHelper) -> K18nMessage)? = null,
    val cancelTimer: Boolean = false
)

enum class TimerSkipResult {
    NEXT, SAME, NOCONCRETE
}

fun TimerSkipResult.defaultData() = TimerSkipData(this)

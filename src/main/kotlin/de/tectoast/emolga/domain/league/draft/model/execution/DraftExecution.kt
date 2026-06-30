package de.tectoast.emolga.domain.league.draft.model.execution

data class DraftExecution(
    val results: List<DraftActionResult>,
    val snipeMap: Map<Int, SnipeMeta>,
    val timerOption: TimerOption,
    val idxToAnnounce: Int?
)

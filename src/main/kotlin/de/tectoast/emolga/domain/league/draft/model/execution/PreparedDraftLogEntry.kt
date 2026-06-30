package de.tectoast.emolga.domain.league.draft.model.execution

data class PreparedDraftLogEntry(val round: Int, val idx: Int, val entry: DraftLogEntry)

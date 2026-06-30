package de.tectoast.emolga.domain.league.draft.model.core

data class DraftActionContext(
    var saveTier: String? = null, var freePick: Boolean = false
)
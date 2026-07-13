package de.tectoast.emolga.domain.league.draft.model.doc

import de.tectoast.emolga.domain.league.doc.model.SheetTemplateData
import kotlinx.serialization.Serializable

@Serializable
data class DraftDocConfig(
    val pick: SheetTemplateData? = null,
    val switch: SheetTemplateData? = null,
    val ban: SheetTemplateData? = null
)
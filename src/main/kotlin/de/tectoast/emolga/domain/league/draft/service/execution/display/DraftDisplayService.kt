package de.tectoast.emolga.domain.league.draft.service.execution.display

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.execution.DraftExecution
import de.tectoast.k18n.generated.K18nLanguage

interface DraftDisplayService {
    suspend fun handleDraftExecution(
        ctx: DraftRunContext,
        execution: DraftExecution,
        modifiedRounds: Set<Int>,
        language: K18nLanguage
    )
}

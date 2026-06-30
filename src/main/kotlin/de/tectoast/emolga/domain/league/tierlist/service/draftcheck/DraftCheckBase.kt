package de.tectoast.emolga.domain.league.tierlist.service.draftcheck

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.DraftCheck
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.handler.BaseHandler

interface DraftCheckOperations<C : DraftCheck> {
    context(data: ValidationRelevantData)
    suspend fun check(config: C, action: DraftAction): ErrorOrNull
}

interface DraftCheckHandler<C : DraftCheck> : BaseHandler<C>, DraftCheckOperations<C>
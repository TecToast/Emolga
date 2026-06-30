package de.tectoast.emolga.domain.league.draft.service.random.pickmode

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickMode
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.handler.BaseHandler

interface RandomPickModeOperations<C : RandomPickMode> {
    suspend fun getRandomPick(
        config: C,
        ctx: DraftRunContext,
        input: RandomPickUserInput,
        validationRelevantData: ValidationRelevantData,
    ): CalcResult<Pair<ShowdownID, String>>
}

interface RandomPickModeHandler<C : RandomPickMode> : RandomPickModeOperations<C>, BaseHandler<C>
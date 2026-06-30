package de.tectoast.emolga.domain.league.draft.service.random.pickmode

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickMode
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class RandomPickModeDispatcher(handlers: List<RandomPickModeHandler<RandomPickMode>>) :
    RandomPickModeOperations<RandomPickMode> {
    private val registry = HandlerRegistry(handlers)

    override suspend fun getRandomPick(
        config: RandomPickMode,
        ctx: DraftRunContext,
        input: RandomPickUserInput,
        validationRelevantData: ValidationRelevantData,
    ): CalcResult<Pair<ShowdownID, String>> {
        val config = ctx.config.randomPick.mode
        return registry.getHandler(config).getRandomPick(config, ctx, input, validationRelevantData)
    }

}
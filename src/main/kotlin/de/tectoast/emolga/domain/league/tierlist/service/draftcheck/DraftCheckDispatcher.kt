package de.tectoast.emolga.domain.league.tierlist.service.draftcheck

import de.tectoast.emolga.domain.league.draft.model.core.DraftAction
import de.tectoast.emolga.domain.league.draft.model.core.ValidationRelevantData
import de.tectoast.emolga.domain.league.tierlist.model.DraftCheck
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class DraftCheckDispatcher(
    handlers: List<DraftCheckHandler<DraftCheck>>
) : DraftCheckOperations<DraftCheck> {
    private val registry = HandlerRegistry(handlers)

    context(data: ValidationRelevantData)
    override suspend fun check(
        config: DraftCheck,
        action: DraftAction
    ): ErrorOrNull {
        return registry.getHandler(config).check(config, action)
    }
}
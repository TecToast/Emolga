package de.tectoast.emolga.domain.league.draft.service.execution.display.message

import de.tectoast.emolga.domain.league.draft.model.execution.DraftLogEntry
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class DraftLogEntryMessageDispatcher(handlers: List<DraftLogEntryMessageHandler<DraftLogEntry>>) :
    DraftLogEntryMessageOperations<DraftLogEntry> {
    private val registry = HandlerRegistry(handlers)
    override suspend fun createMessage(
        entry: DraftLogEntry,
        userRef: String,
        pokemonDisplayFn: suspend (ShowdownID) -> String
    ) = registry.getHandler(entry).createMessage(entry, userRef, pokemonDisplayFn)
}
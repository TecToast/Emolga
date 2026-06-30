package de.tectoast.emolga.domain.league.draft.service.execution.display.message

import de.tectoast.emolga.domain.league.draft.model.execution.DraftLogEntry
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.handler.BaseHandler
import de.tectoast.k18n.generated.K18nMessage

interface DraftLogEntryMessageOperations<C : DraftLogEntry> {
    suspend fun createMessage(entry: C, userRef: String, pokemonDisplayFn: suspend (ShowdownID) -> String): K18nMessage
}

interface DraftLogEntryMessageHandler<C : DraftLogEntry> : DraftLogEntryMessageOperations<C>, BaseHandler<C>
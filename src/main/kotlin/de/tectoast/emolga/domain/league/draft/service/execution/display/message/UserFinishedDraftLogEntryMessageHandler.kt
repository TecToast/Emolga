package de.tectoast.emolga.domain.league.draft.service.execution.display.message

import de.tectoast.emolga.domain.league.draft.model.execution.DraftLogEntry
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.invoke
import de.tectoast.generic.K18n_Finished
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class UserFinishedDraftLogEntryMessageHandler : DraftLogEntryMessageHandler<DraftLogEntry.UserFinished> {
    override val targetClass = DraftLogEntry.UserFinished::class

    override suspend fun createMessage(
        entry: DraftLogEntry.UserFinished,
        userRef: String,
        pokemonDisplayFn: suspend (ShowdownID) -> String
    ): K18nMessage {
        return b {
            "${K18n_Finished()} ($userRef)"
        }
    }
}
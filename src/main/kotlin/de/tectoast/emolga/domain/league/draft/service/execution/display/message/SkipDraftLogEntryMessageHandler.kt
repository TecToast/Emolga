package de.tectoast.emolga.domain.league.draft.service.execution.display.message

import de.tectoast.emolga.domain.league.draft.model.core.SkipReason
import de.tectoast.emolga.domain.league.draft.model.execution.DraftLogEntry
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.draft.K18n_DraftUtils
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.k18n
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class SkipDraftLogEntryMessageHandler : DraftLogEntryMessageHandler<DraftLogEntry.Skip> {
    override val targetClass = DraftLogEntry.Skip::class

    override suspend fun createMessage(
        entry: DraftLogEntry.Skip,
        userRef: String,
        pokemonDisplayFn: suspend (ShowdownID) -> String
    ): K18nMessage = with(entry) {
        val skippedBy = (reason as? SkipReason.Skip)?.skippedByExternal
        val base = "N/A" + (skippedBy?.let { " <- <@$skippedBy>" } ?: "") + " ($userRef)"
        if (madeUpRound == null) return base.k18n
        b {
            "$base [${K18n_DraftUtils.MadeUpInRound(madeUpRound)()}]"
        }
    }
}
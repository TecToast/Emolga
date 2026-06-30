package de.tectoast.emolga.domain.league.draft.service.execution.display.message

import de.tectoast.emolga.domain.league.draft.model.core.BanInput
import de.tectoast.emolga.domain.league.draft.model.core.DraftActionOrigin
import de.tectoast.emolga.domain.league.draft.model.core.PickInput
import de.tectoast.emolga.domain.league.draft.model.core.SwitchInput
import de.tectoast.emolga.domain.league.draft.model.execution.DraftLogEntry
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.draft.K18n_DraftUtils
import de.tectoast.emolga.utils.invoke
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class ActionDraftLogEntryMessageHandler : DraftLogEntryMessageHandler<DraftLogEntry.Action> {

    override val targetClass = DraftLogEntry.Action::class

    override suspend fun createMessage(
        entry: DraftLogEntry.Action,
        userRef: String,
        pokemonDisplayFn: suspend (ShowdownID) -> String
    ): K18nMessage = with(entry) {
        val actionText = when (input) {
            is PickInput -> pokemonDisplayFn(input.pokemon)
            is SwitchInput -> "${pokemonDisplayFn(input.oldmon)} -> ${pokemonDisplayFn(input.pokemon)}"
            is BanInput -> "BAN ${pokemonDisplayFn(input.pokemon)}"
        }
        return b {
            buildString {
                append(actionText)
                showTier?.let { append(" ($it)") }
                append(" ($userRef)")
                if (forRound != null) append(" [${K18n_DraftUtils.PickedForRound(forRound)()}]")
                when (type) {
                    DraftActionOrigin.REGULAR -> {}
                    DraftActionOrigin.QUEUE -> {
                        append(" [Queue]")
                    }

                    DraftActionOrigin.RANDOM -> {
                        append(" [Random]")
                    }

                    DraftActionOrigin.ACCEPT -> {
                        append(" [Accept]")
                    }

                    DraftActionOrigin.REROLL -> {
                        append(" [Reroll]")
                    }
                }
            }
        }
    }
}
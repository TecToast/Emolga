package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.draft.DraftMessageType
import de.tectoast.emolga.utils.draft.DraftUtils
import de.tectoast.emolga.utils.draft.SwitchInput
import mu.KotlinLogging

object SwitchCommand :
    CommandFeature<SwitchCommand.Args>(::Args, CommandSpec("switch", "Switcht ein Pokemon")) {

    private val logger = KotlinLogging.logger {}

    class Args : Arguments() {
        var oldmon by draftPokemon(
            "Altes Mon",
            "Das Pokemon, was rausgeschmissen werden soll",
            autocomplete = { s, event ->
                val league = League.onlyChannel(event.channelIdLong) ?: return@draftPokemon null
                val current = league.currentOrFromID(event.user.idLong) ?: return@draftPokemon null
                monOfTeam(s, league, current)
            }
        )
        var newmon by draftPokemon("Neues Mon", "Das Pokemon, was stattdessen reinkommen soll")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        League.executePickLike {
            DraftUtils.executeWithinLock(SwitchInput(e.oldmon, e.newmon), DraftMessageType.REGULAR)
        }
    }

}

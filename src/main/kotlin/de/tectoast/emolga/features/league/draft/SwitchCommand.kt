package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.database.league.DraftMessageType
import de.tectoast.emolga.database.league.DraftService
import de.tectoast.emolga.database.league.SwitchInput
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.json.isError

class SwitchCommand(val draftService: DraftService) :
    CommandFeature<SwitchCommand.Args>(::Args, CommandSpec("switch", K18n_Switch.Help)) {

    // TODO autocomplete for mons

    class Args : Arguments() {
        var oldmon by draftPokemon(
            "Altes Mon",
            K18n_Switch.ArgOldMon,
        )
        var newmon by draftPokemon("Neues Mon", K18n_Switch.ArgNewMon)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(ephemeral = true)
        val validationCompleteCallback = suspend {
            iData.reply("\uD83D\uDC4D", ephemeral = true)
        }
        val result = draftService.executeNormal(
            SwitchInput(e.oldmon.showdownId, e.newmon),
            DraftMessageType.REGULAR,
            iData.tc,
            iData.user,
            iData.member().unsortedRoles.mapNotNull { it.idLong }.toSet(),
            validationCompleteCallback
        )
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }

}

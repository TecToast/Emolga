package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.database.league.BanInput
import de.tectoast.emolga.database.league.DraftMessageType
import de.tectoast.emolga.database.league.DraftService
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.json.isError

class BanMonCommand(val draftService: DraftService) : CommandFeature<BanMonCommand.Args>(
    ::Args,
    CommandSpec("banmon", K18n_BanMon.Help)
) {
    class Args : Arguments() {
        var pokemon by draftPokemon("Pokemon", K18n_BanMon.ArgPokemon)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(ephemeral = true)
        val validationCompleteCallback = suspend {
            iData.reply("\uD83D\uDC4D", ephemeral = true)
        }
        val result = draftService.executeNormal(
            BanInput(e.pokemon),
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

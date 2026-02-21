package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.draft.BanInput
import de.tectoast.emolga.utils.draft.DraftMessageType
import de.tectoast.emolga.utils.draft.DraftUtils

object BanMonCommand : CommandFeature<BanMonCommand.Args>(
    ::Args,
    CommandSpec("banmon", K18n_BanMon.Help)
) {
    class Args : Arguments() {
        var pokemon by draftPokemon("Pokemon", K18n_BanMon.ArgPokemon)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        League.executePickLike {
            DraftUtils.executeWithinLock(
                BanInput(e.pokemon),
                DraftMessageType.REGULAR
            )
        }
    }
}

package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.BanInput
import de.tectoast.emolga.utils.draft.DraftMessageType
import de.tectoast.emolga.utils.draft.DraftUtils
import de.tectoast.emolga.league.League

object BanMonCommand : CommandFeature<BanMonCommand.Args>(
    ::Args,
    CommandSpec("banmon", "Bannt ein Mon im Pick&Ban-System", Constants.G.FLP, Constants.G.EPP)
) {
    class Args : Arguments() {
        var pokemon by draftPokemon("Pokemon", "Das Pokemon, welches du bannen möchtest")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        League.executePickLike {
            DraftUtils.executeWithinLock(
                BanInput(e.pokemon),
                DraftMessageType.REGULAR
            )
        }
    }
}

package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NextPlayerData
import de.tectoast.emolga.league.SkipReason

object SkipCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("skip", K18n_Skip.Help)) {

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executePickLike l@{
            if (!isSwitchDraft) {
                iData.reply(K18n_Skip.IsNoSwitchDraft)
                return@l
            }
            this.replyGeneral(K18n_Skip.Success)
            afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, current))
        }
    }
}

package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.league.AFTER_DRAFT_UNORDERED
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NextPlayerData
import de.tectoast.emolga.league.SkipReason

object MoveCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("move", K18n_Move.Help)) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executePickLike l@{
            if (isSwitchDraft) {
                return@l iData.reply(K18n_Move.IsSwitchDraft)
            }
            if (pseudoEnd && afterTimerSkipMode == AFTER_DRAFT_UNORDERED) {
                return@l iData.reply(K18n_Move.InPseudoEnd)
            }
            replyGeneral(K18n_Move.Success)
            afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, skippedUser = current))
        }
    }
}

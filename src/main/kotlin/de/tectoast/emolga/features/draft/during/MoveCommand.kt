package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.league.AFTER_DRAFT_UNORDERED
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NextPlayerData
import de.tectoast.emolga.league.SkipReason

object MoveCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("move", "Verschiebt deinen Pick")) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executePickLike {
            if (isSwitchDraft) {
                return iData.reply("Dieser Draft ist ein Switch-Draft, daher wird /move nicht unterstützt!")
            }
            if (pseudoEnd && afterTimerSkipMode == AFTER_DRAFT_UNORDERED) {
                return iData.reply("Der Draft ist quasi schon vorbei, du kannst jetzt nicht mehr moven!")
            }
            replyGeneral("den Pick verschoben!")
            afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, skippedUser = current))
        }
    }
}

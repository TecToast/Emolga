package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NextPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.SkipReason

object SkipCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("skip", "Überspringt deinen Zug", *draftGuilds)) {

    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executePickLike {
            if (!isSwitchDraft) {
                reply("Dieser Draft ist kein Switch-Draft, daher wird /skip nicht unterstützt!")
                return
            }
            replySkip()
            afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, current))
        }
    }
}

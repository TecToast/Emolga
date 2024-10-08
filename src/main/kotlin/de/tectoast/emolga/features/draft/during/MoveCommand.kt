package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NextPlayerData
import de.tectoast.emolga.utils.json.emolga.draft.SkipReason

object MoveCommand : CommandFeature<NoArgs>(NoArgs(), CommandSpec("move", "Verschiebt deinen Pick", *draftGuilds)) {
    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executePickLike {
            if (isSwitchDraft) {
                return reply("Dieser Draft ist ein Switch-Draft, daher wird /move nicht unterstützt!")
            }
            if (pseudoEnd) {
                return reply("Der Draft ist quasi schon vorbei, du kannst jetzt nicht mehr moven!")
            }
            replyGeneral("den Pick verschoben!")
            afterPickOfficial(NextPlayerData.Moved(SkipReason.SKIP, skippedUser = current))
        }
    }
}

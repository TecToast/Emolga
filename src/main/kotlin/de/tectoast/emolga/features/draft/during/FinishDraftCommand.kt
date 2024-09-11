package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.json.emolga.draft.League

object FinishDraftCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("finishdraft", "Beendet für dich den Draft", *draftGuilds)) {
    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executeAsNotCurrent(asParticipant = true) {
            if (isFinishedForbidden()) return reply("Dieser Draft unterstützt /finishdraft nicht!")
            val idx = this(user)
            checkFinishedForbidden(idx)?.let {
                return reply(it)
            }
            replyAwait("<@${user}> hat den Draft für sich beendet!")
            addFinished(idx)
            if (current == idx)
                afterPickOfficial()
            else
                save("FinishDraft")
        }
    }
}

package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.league.League

object FinishDraftCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("finishdraft", "Beendet für dich den Draft")) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executeAsNotCurrent(asParticipant = true) {
            if (isFinishedForbidden()) return iData.reply("Dieser Draft unterstützt /finishdraft nicht!")
            val idx = this(iData.user)
            checkFinishedForbidden(idx)?.let {
                return iData.reply(it)
            }
            iData.replyAwait("<@${iData.user}> hat den Draft für sich beendet!")
            addFinished(idx)
            if (current == idx)
                afterPickOfficial()
            else
                save("FinishDraft")
        }
    }
}

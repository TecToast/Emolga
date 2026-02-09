package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.league.League

object FinishDraftCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("finishdraft", K18n_FinishDraft.Help)) {
    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        League.executeAsNotCurrent(asParticipant = true) l@{
            if (isFinishedForbidden()) return@l iData.reply(K18n_FinishDraft.NoSupport)
            val idx = this(iData.user)
            checkFinishedForbidden(idx)?.let {
                return@l iData.reply(it)
            }
            iData.replyAwait(K18n_FinishDraft.Success(iData.user))
            val wasCurrent = addFinished(idx)
            if (wasCurrent)
                afterPickOfficial()
            else
                save()
        }
    }
}

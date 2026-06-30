package de.tectoast.emolga.domain.league.draft.util

import de.tectoast.emolga.domain.league.member.model.MessageMentionData
import de.tectoast.k18n.generated.K18nMessage

interface DisplayHelper {
    suspend fun buildAnnounceData(idx: Int, withTimerAnnounce: Boolean): K18nMessage
    suspend fun getPingForUser(idx: Int): MessageMentionData
}

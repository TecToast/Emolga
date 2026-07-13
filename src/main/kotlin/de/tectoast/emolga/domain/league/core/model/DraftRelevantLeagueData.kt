package de.tectoast.emolga.domain.league.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DraftRelevantLeagueData(
    val leagueName: String,
    val displayName: String,
    val guild: Long,
    val sheetId: String,
    val draftChannel: Long,
    val draftOrder: Map<Int, List<Int>>,
    val isSwitchDraft: Boolean,
    var draftData: ResettableLeagueData
) {
    val pseudoEnd get() = draftData.draftState == DraftState.PSEUDOEND
    val round get() = draftData.round
    val totalRounds get() = draftOrder.size
    private val isLastRound get() = round == totalRounds
    val draftWouldEnd get() = isLastRound && indexInRound == draftOrder[round]!!.lastIndex
    val indexInRound get() = draftData.indexInRound
    val currentIdx get() = draftOrder[round]!![indexInRound]
    val alreadyBannedMonsThisRound get() = draftData.draftBan.bannedMons[round].orEmpty()

    fun hasMovedTurns(idx: Int) = movedTurns(idx).isNotEmpty()
    fun movedTurns(idx: Int) = draftData.moved[idx] ?: mutableListOf()
    fun addToMoved(idx: Int) {
        if (!isSwitchDraft) draftData.moved.getOrPut(idx) { mutableListOf() }.let { if (round !in it) it += round }
    }
}

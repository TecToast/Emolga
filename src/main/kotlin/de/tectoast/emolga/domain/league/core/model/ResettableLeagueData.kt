package de.tectoast.emolga.domain.league.core.model

import de.tectoast.emolga.domain.league.draft.model.ban.DraftBanData
import de.tectoast.emolga.domain.league.draft.model.random.RandomLeagueData
import de.tectoast.emolga.domain.league.draft.model.timer.TimerRelated
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class ResettableLeagueData(
    val draftBan: DraftBanData = DraftBanData(),
    val randomPick: RandomLeagueData = RandomLeagueData(),
    val timer: TimerRelated = TimerRelated(),
    val moved: MutableMap<Int, MutableList<Int>> = mutableMapOf(),
    val punishableSkippedTurns: MutableMap<Int, MutableSet<Int>> = mutableMapOf(),
    val finishedDraft: MutableSet<Int> = mutableSetOf(),
    var round: Int = 1,
    var indexInRound: Int = 0,
    var draftSessionNum: Int = 0,
    @EncodeDefault
    var draftState: DraftState = DraftState.OFF,
)
package de.tectoast.emolga.league.config

import de.tectoast.emolga.features.draft.TipGame
import de.tectoast.emolga.utils.DraftTimer
import kotlinx.serialization.Serializable

@Serializable
data class LeagueConfig(
    var timer: DraftTimer? = null,
    val replayDataStore: ReplayDataStoreConfig? = null,
    val tipgame: TipGame? = null,
    val allowPickDuringSwitch: AllowPickDuringSwitchConfig = AllowPickDuringSwitchConfig(),
    val draftBan: DraftBanConfig? = null,
    val draftSend: DraftSendConfig = DraftSendConfig(),
    val queuePicks: QueuePicksConfig = QueuePicksConfig(),
    val randomPick: RandomPickConfig = RandomPickConfig(),
    val randomPickRound: RandomPickRoundConfig? = null,
    val teraAndZ: TeraAndZ? = null,
    val youtube: YouTubeConfig? = null,
)


@Serializable
data class ResettableLeagueData(
    val draftBan: DraftBanData = DraftBanData(),
    val randomPickRound: RandomPickRoundData = RandomPickRoundData(),
    val randomPick: RandomLeagueData = RandomLeagueData(),
    val timer: TimerRelated = TimerRelated(),
)

@Serializable
data class PersistentLeagueData(
    val replayDataStore: ReplayDataStoreData = ReplayDataStoreData(),
    val queuePicks: QueuePicksData = QueuePicksData(),
)
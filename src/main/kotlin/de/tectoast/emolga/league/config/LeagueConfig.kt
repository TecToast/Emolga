package de.tectoast.emolga.league.config

import de.tectoast.emolga.features.league.PredictionGame
import de.tectoast.emolga.utils.DraftTimer
import kotlinx.serialization.Serializable

@Serializable
data class LeagueConfig(
    var timer: DraftTimer? = null,
    val replayDataStore: ReplayDataStoreConfig? = null,
    val predictionGame: PredictionGame? = null,
    val draftBan: DraftBanConfig? = null,
    val randomPick: RandomPickConfig = RandomPickConfig(),
    val randomPickRound: RandomPickRoundConfig? = null,
    val teraAndZ: TeraAndZ? = null,
    val youtube: YouTubeConfig? = null,
    val customTierlist: CustomTierlistConfig? = null,
    val teraPick: TeraPickConfig? = null,
    val triggers: Triggers = Triggers(),
    val hideGames: HideGamesConfig? = null,
    val teamgraphics: TeamGraphicsLeagueConfig? = null
)


@Serializable
data class ResettableLeagueData(
    val draftBan: DraftBanData = DraftBanData(),
    val randomPickRound: RandomPickRoundData = RandomPickRoundData(),
    val randomPick: RandomLeagueData = RandomLeagueData(),
    val timer: TimerRelated = TimerRelated(),
    val teraPick: TeraPickData = TeraPickData(),
    val moved: MutableMap<Int, MutableList<Int>> = mutableMapOf(),
    val punishableSkippedTurns: MutableMap<Int, MutableSet<Int>> = mutableMapOf(),
    var round: Int = 1,
)

@Serializable
data class PersistentLeagueData(
    val replayDataStore: ReplayDataStoreData = ReplayDataStoreData(),
    val queuePicks: QueuePicksData = QueuePicksData(),
    val teamReveal: TeamRevealData = TeamRevealData(),
)
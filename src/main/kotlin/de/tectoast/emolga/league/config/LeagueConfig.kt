package de.tectoast.emolga.league.config

import de.tectoast.emolga.features.league.PredictionGameConfig
import de.tectoast.emolga.league.DraftState
import de.tectoast.emolga.utils.DraftTimer
import kotlinx.serialization.Serializable


data class LeagueConfig(
    var timer: DraftTimer? = null,
    val replayDataStore: ReplayDataStoreConfig? = null,
    val predictionGame: PredictionGameConfig? = null,
    val draftBan: DraftBanConfig? = null,
    val randomPick: RandomPickConfig = RandomPickConfig(),
    val randomPickRound: RandomPickRoundConfig? = null,
    val teraAndZ: TeraAndZ? = null,
    val youtube: YouTubeConfig? = null,
    val customTierlist: CustomTierlistConfig? = null,
    val customSignup: CustomSignupConfig? = null,
    val teraPick: TeraPickConfig? = null,
    val triggers: Triggers = Triggers(),
    val hideGames: HideGamesConfig? = null,
    val teamgraphics: TeamGraphicsLeagueConfig? = null,
    val transaction: TransactionConfig? = null,
    val sheetTemplateId: String? = null
) {
    val tlIdentifier get() = customTierlist?.identifier ?: ""

    operator fun plus(other: LeagueConfigOverride?): LeagueConfig {
        if(other == null) return this
        return LeagueConfig(
            timer = other.timer ?: timer,
            replayDataStore = other.replayDataStore ?: replayDataStore,
            predictionGame = other.predictionGame ?: predictionGame,
            draftBan = other.draftBan ?: draftBan,
            randomPick = randomPick + other.randomPick,
            randomPickRound = other.randomPickRound ?: randomPickRound,
            teraAndZ = other.teraAndZ ?: teraAndZ,
            youtube = other.youtube ?: youtube,
            customTierlist = other.customTierlist ?: customTierlist,
            customSignup = other.customSignup ?: customSignup,
            teraPick = other.teraPick ?: teraPick,
            triggers = triggers + other.triggers,
            hideGames = other.hideGames ?: hideGames,
            teamgraphics = other.teamgraphics ?: teamgraphics,
            transaction = other.transaction ?: transaction
        )
    }
}

@Serializable
data class LeagueConfigOverride(
    var timer: DraftTimer? = null,
    val replayDataStore: ReplayDataStoreConfig? = null,
    val predictionGame: PredictionGameConfig? = null,
    val draftBan: DraftBanConfig? = null,
    val randomPick: RandomPickConfigOverride? = null,
    val randomPickRound: RandomPickRoundConfig? = null,
    val teraAndZ: TeraAndZ? = null,
    val youtube: YouTubeConfig? = null,
    val customTierlist: CustomTierlistConfig? = null,
    val customSignup: CustomSignupConfig? = null,
    val teraPick: TeraPickConfig? = null,
    val triggers: TriggersOverride? = null,
    val hideGames: HideGamesConfig? = null,
    val teamgraphics: TeamGraphicsLeagueConfig? = null,
    val transaction: TransactionConfig? = null,
)


@Serializable
data class ResettableLeagueData(
    val draftBan: DraftBanData = DraftBanData(),
    val randomPickRound: RandomPickRoundData = RandomPickRoundData(),
    val randomPick: RandomLeagueData = RandomLeagueData(),
    val timer: TimerRelated = TimerRelated(),
    val moved: MutableMap<Int, MutableList<Int>> = mutableMapOf(),
    val punishableSkippedTurns: MutableMap<Int, MutableSet<Int>> = mutableMapOf(),
    var round: Int = 1,
    var indexInRound: Int = 0,
    var draftState: DraftState = DraftState.OFF
)

@Serializable
data class PersistentLeagueData(
    val teamReveal: TeamRevealData = TeamRevealData(),
    val transaction: LeagueTransactionData = LeagueTransactionData(),
)
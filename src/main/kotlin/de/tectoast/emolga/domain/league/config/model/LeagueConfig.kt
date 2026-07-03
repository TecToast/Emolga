package de.tectoast.emolga.domain.league.config.model

import de.tectoast.emolga.domain.game.model.GameDataStoreConfig
import de.tectoast.emolga.domain.league.doc.model.HideGamesConfig
import de.tectoast.emolga.domain.league.doc.model.MonsDocOrderConfig
import de.tectoast.emolga.domain.league.doc.model.StatProcessor
import de.tectoast.emolga.domain.league.draft.model.ban.DraftBanConfig
import de.tectoast.emolga.domain.league.draft.model.config.TeraPickConfig
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickConfig
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickConfigOverride
import de.tectoast.emolga.domain.league.draft.model.timer.DraftTimerConfig
import de.tectoast.emolga.domain.league.prediction.model.config.PredictionGameConfig
import de.tectoast.emolga.domain.league.signup.model.CustomSignupConfig
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamGraphicLeagueConfig
import de.tectoast.emolga.domain.league.tierlist.model.CustomTierlistConfig
import de.tectoast.emolga.domain.league.transaction.model.TransactionConfig
import de.tectoast.emolga.domain.league.youtube.model.YouTubeConfig
import kotlinx.serialization.Serializable


@Serializable
data class LeagueConfig(
    var timer: DraftTimerConfig? = null,
    val gameDataStore: GameDataStoreConfig? = null,
    val predictionGame: PredictionGameConfig? = null,
    val draftBan: DraftBanConfig? = null,
    val randomPick: RandomPickConfig = RandomPickConfig(),
    val youtube: YouTubeConfig? = null,
    val customTierlist: CustomTierlistConfig? = null,
    val customSignup: CustomSignupConfig? = null,
    val teraPick: TeraPickConfig? = null,
    val triggers: Triggers = Triggers(),
    val hideGames: HideGamesConfig? = null,
    val teamgraphics: TeamGraphicLeagueConfig? = null,
    val transaction: TransactionConfig? = null,
    val sheetTemplateId: String? = null,
    val teamSize: Int = 11,
    val replayChannel: Long? = null,
    val resultChannel: Long? = null,
    val monsDocOrder: MonsDocOrderConfig = MonsDocOrderConfig.PickOrder,
    val statProcessors: Set<StatProcessor> = emptySet(),
) {
    val tlIdentifier get() = customTierlist?.identifier ?: ""

    operator fun plus(other: LeagueConfigOverride?): LeagueConfig {
        if (other == null) return this
        return LeagueConfig(
            timer = other.timer ?: timer,
            gameDataStore = other.replayDataStore ?: gameDataStore,
            predictionGame = other.predictionGame ?: predictionGame,
            draftBan = other.draftBan ?: draftBan,
            randomPick = randomPick + other.randomPick,
            youtube = other.youtube ?: youtube,
            customTierlist = other.customTierlist ?: customTierlist,
            customSignup = other.customSignup ?: customSignup,
            teraPick = other.teraPick ?: teraPick,
            triggers = triggers + other.triggers,
            hideGames = other.hideGames ?: hideGames,
            teamgraphics = other.teamgraphics ?: teamgraphics,
            transaction = other.transaction ?: transaction,
            sheetTemplateId = other.sheetTemplateId ?: sheetTemplateId,
            teamSize = other.teamSize ?: teamSize,
            replayChannel = other.replayChannel ?: replayChannel,
            resultChannel = other.resultChannel ?: resultChannel,
            monsDocOrder = other.monsDocOrder ?: monsDocOrder,
            statProcessors = other.statProcessors ?: statProcessors,
        )
    }
}

@Serializable
data class LeagueConfigOverride(
    var timer: DraftTimerConfig? = null,
    val replayDataStore: GameDataStoreConfig? = null,
    val predictionGame: PredictionGameConfig? = null,
    val draftBan: DraftBanConfig? = null,
    val randomPick: RandomPickConfigOverride? = null,
    val youtube: YouTubeConfig? = null,
    val customTierlist: CustomTierlistConfig? = null,
    val customSignup: CustomSignupConfig? = null,
    val teraPick: TeraPickConfig? = null,
    val triggers: TriggersOverride? = null,
    val hideGames: HideGamesConfig? = null,
    val teamgraphics: TeamGraphicLeagueConfig? = null,
    val transaction: TransactionConfig? = null,
    val sheetTemplateId: String? = null,
    val teamSize: Int? = null,
    val replayChannel: Long? = null,
    val resultChannel: Long? = null,
    val monsDocOrder: MonsDocOrderConfig? = null,
    val statProcessors: Set<StatProcessor>? = null,
)





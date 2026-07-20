package de.tectoast.emolga.domain.league.prediction.service

import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.gamedata.repository.GameDataRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.prediction.model.PredictionMatchViewState
import de.tectoast.emolga.domain.league.prediction.model.config.PredictionGameConfig
import de.tectoast.emolga.domain.league.prediction.model.config.PredictionGameCurrentStateType
import de.tectoast.emolga.domain.league.prediction.repository.PredictionGameMessageRepository
import de.tectoast.emolga.domain.league.prediction.repository.PredictionGameVoteRepository
import de.tectoast.emolga.domain.league.prediction.service.bridge.PredictionGameUI
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import de.tectoast.emolga.features.league.K18n_PredictionGame
import de.tectoast.emolga.utils.Constants
import de.tectoast.generic.K18n_Week
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.coroutines.*
import org.koin.core.annotation.Single
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@Single
class PredictionGameService(
    private val predictionGameMessageRepo: PredictionGameMessageRepository,
    private val predictionGameVotesRepo: PredictionGameVoteRepository,
    private val predictionGameAnalyseService: PredictionGameAnalyseService,
    private val discordUserService: DiscordUserService,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val languageRepo: GuildConfigRepository,
    private val gameDataRepo: GameDataRepository,
    private val ui: PredictionGameUI,
    baseScope: CoroutineScope
) {
    val scope = baseScope + CoroutineName("PredictionGame")

    fun updateCorrectBattles(leagueName: String, week: Int, battleIndex: Int, winnerIdx: Int) {
        scope.launch {
            predictionGameVotesRepo.updateCorrectBattles(leagueName, week, battleIndex, winnerIdx)
        }
    }

    fun lockButtons(leagueName: String, week: Int) {
        scope.launch {
            val matchUps = leagueScheduleRepo.getMatchUpsForWeek(leagueName, week)
            for (battleIndex in matchUps.map { it.battleIndex }) {
                updatePredictionGameMessage(leagueName, week, battleIndex, PredictionGameMessageEditSource.LOCK)
            }
        }
    }

    fun lockButtonsIndividual(
        leagueName: String,
        week: Int,
        battleIndex: Int
    ) {
        scope.launch {
            updatePredictionGameMessage(leagueName, week, battleIndex, PredictionGameMessageEditSource.LOCK)
        }
    }

    private suspend fun updatePredictionGameMessage(
        leagueName: String,
        week: Int,
        battleIndex: Int,
        source: PredictionGameMessageEditSource
    ) {
        val messageId =
            predictionGameMessageRepo.getMessageIds(leagueName, week, battleIndex).firstOrNull() ?: return
        val guildId = leagueCoreRepo.getScalarLeagueData(leagueName).guild
        val config = leagueConfigRepo.getConfig(leagueName).predictionGame ?: return
        val matchUp = leagueScheduleRepo.getMatchUp(leagueName, week, battleIndex) ?: return
        val users = leagueMemberRepo.getPrimaryIds(leagueName, matchUp)
        val names = discordUserService.getNames(guildId, users.values.flatten())
        val language = languageRepo.getLanguage(guildId)
        val state = buildPredictionMatchViewState(
            leagueName = leagueName,
            week = week,
            battleIndex = battleIndex,
            channel = config.channel,
            matchUp = matchUp,
            users = users,
            names = names,
            config = config,
            isLocked = source == PredictionGameMessageEditSource.LOCK,
            description = if (source.targetState == config.currentState) buildCurrentPredictionGameState(
                leagueName,
                week,
                battleIndex,
                matchUp
            ).translateTo(language) else null
        )
        ui.updatePredictionGameMessage(state, messageId)
    }

    enum class PredictionGameMessageEditSource(val targetState: PredictionGameCurrentStateType) {
        VOTE(PredictionGameCurrentStateType.ALWAYS), LOCK(PredictionGameCurrentStateType.ON_LOCK)
    }

    fun send(leagueName: String, week: Int, channelId: Long? = null) {
        scope.launch {
            val scalarLeagueData = leagueCoreRepo.getScalarLeagueData(leagueName)
            val guildId = scalarLeagueData.guild
            val prettyName = scalarLeagueData.prettyName
            val config = leagueConfigRepo.getConfig(leagueName).predictionGame ?: return@launch
            val matchUps = leagueScheduleRepo.getMatchUpsForWeek(leagueName, week)
            val users = leagueMemberRepo.getPrimaryIds(leagueName)
            val names = discordUserService.getNames(guildId, users.values.flatten())
            val channel = channelId ?: config.channel
            val language = languageRepo.getLanguage(guildId)
            val playedGames = gameDataRepo.getPlayedGames(leagueName, week)
            val titleEmbedColor = config.customEmbedColor ?: Color.YELLOW.rgb
            ui.sendInitialMessage(
                channel,
                buildString {
                    append(K18n_Week.translateTo(language))
                    append(" ")
                    append(week)
                    if (prettyName != null) {
                        append(" - ")
                        append(prettyName)
                    }
                },
                titleEmbedColor
            )
            for (matchUp in matchUps) {
                val state = buildPredictionMatchViewState(
                    leagueName = leagueName,
                    week = week,
                    battleIndex = matchUp.battleIndex,
                    channel = channel,
                    matchUp = matchUp.indices,
                    users = users,
                    names = names,
                    config = config,
                    isLocked = matchUp.battleIndex in playedGames,
                    description = if (config.currentState == PredictionGameCurrentStateType.ALWAYS) K18n_PredictionGame.VotesUntilNow(
                        "0:0"
                    ).translateTo(language) else null
                )
                val messageId = ui.sendPredictionGameMessage(state)
                predictionGameMessageRepo.setMessageId(leagueName, week, matchUp.battleIndex, messageId)
                delay(1.seconds)
            }
            config.roleToPing?.let { roleId ->
                ui.sendRolePing(channel, roleId)
            }
        }
    }

    private suspend fun buildCurrentPredictionGameState(
        leagueName: String, week: Int, battleIndex: Int, indices: List<Int>?
    ): K18nMessage {
        val indicesToUse = indices ?: leagueScheduleRepo.getMatchUp(leagueName, week, battleIndex).orEmpty()
        val stateMap = predictionGameAnalyseService.getCurrentVoteState(leagueName, week, battleIndex)
        return K18n_PredictionGame.VotesUntilNow(indicesToUse.joinToString(":") {
            stateMap.getOrDefault(it, 0).toString()
        })
    }

    suspend fun addVote(userId: Long, leagueName: String, week: Int, index: Int, userindex: Int): Boolean {
        val config = leagueConfigRepo.getConfig(leagueName).predictionGame ?: return false
        predictionGameVotesRepo.addVote(userId, leagueName, week, index, userindex)
        if (config.currentState == PredictionGameCurrentStateType.ALWAYS) {
            updatePredictionGameMessage(leagueName, week, index, PredictionGameMessageEditSource.VOTE)
        }
        return true
    }

    private fun buildPredictionMatchViewState(
        leagueName: String,
        week: Int,
        battleIndex: Int,
        channel: Long,
        matchUp: List<Int>,
        users: Map<Int, List<Long>>,
        names: Map<Long, String>,
        config: PredictionGameConfig,
        isLocked: Boolean,
        description: String?
    ): PredictionMatchViewState = PredictionMatchViewState(
        leagueName = leagueName,
        week = week,
        battleIndex = battleIndex,
        channelId = channel,
        isLocked = isLocked,
        idx1 = matchUp[0],
        idx2 = matchUp[1],
        player1Name = users[matchUp[0]]!!.joinToString(" & ") { names[it] ?: "N/A" },
        player2Name = users[matchUp[1]]!!.joinToString(" & ") { names[it] ?: "N/A" },
        embedDescription = description,
        embedColor = config.customEmbedColor ?: Constants.EMBED_COLOR
    )
}

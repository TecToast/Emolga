package de.tectoast.emolga.domain.game.service.process

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.discord.*
import de.tectoast.emolga.domain.game.model.FullInputGame
import de.tectoast.emolga.domain.game.model.GameSource
import de.tectoast.emolga.domain.game.model.ResultMessage
import de.tectoast.emolga.domain.language.repository.GuildLanguageRepository
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.doc.service.DocEntryService
import de.tectoast.emolga.domain.league.doc.service.HideGamesInsertFlow
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.domain.league.gamedata.model.GameData
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.schedule.repository.LeagueScheduleRepository
import de.tectoast.emolga.features.league.draft.generic.K18n_NoWritePermissionInChannel
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.joinToTeammates
import de.tectoast.emolga.utils.showdown.K18n_Analysis
import de.tectoast.generic.K18n_UpdateNotice
import de.tectoast.generic.K18n_Week
import de.tectoast.k18n.generated.K18nLanguage
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class GameProcessService(
    private val leagueConfigRepo: LeagueConfigRepository,
    private val leagueScheduleRepo: LeagueScheduleRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val resultMessageBuilder: ResultMessageBuilder,
    private val docEntryService: DocEntryService,
    private val languageRepo: GuildLanguageRepository,
    private val hideGamesInsertFlow: HideGamesInsertFlow,
    private val channelPermissionChecker: ChannelPermissionChecker,
    private val channelInterface: ChannelInterface,
    private val botConstants: BotConstants,
    dispatcher: CoroutineDispatcher
) : StartupTask {

    private val scope = createCoroutineScope("GameProcessService", dispatcher)
    private val logger = KotlinLogging.logger {}

    override suspend fun onStartup() {
        hideGamesInsertFlow.launch(scope) { (games, config, guild) ->
            scope.launch {
                val infoSender = K18nMessageSender {
                    logger.warn("Info/Error sent in hide games insertion: $it")
                }
                val replaySender = channelInterface.createSingleChannel(config.replayChannel)
                val resultChannelParam = config.resultChannel
                games.forEachIndexed { index, game ->
                    analyseGame(
                        fullInputGame = game,
                        infoSender = infoSender,
                        replaySender = replaySender,
                        resultchannelParam = resultChannelParam,
                        guildOfChannel = guild,
                        withSort = index == games.lastIndex,
                        ignoreHideGames = true
                    )
                    delay(3.seconds)
                }
            }
        }
    }

    suspend fun analyseGame(
        fullInputGame: FullInputGame,
        infoSender: K18nMessageSender,
        replaySender: MessageSender,
        resultchannelParam: Long,
        guildOfChannel: Long,
        customGuild: Long? = null,
        withSort: Boolean = true,
        errorSender: K18nMessageSender = infoSender,
        ignoreHideGames: Boolean = false,
    ) {
        val guildId = customGuild ?: guildOfChannel
        val language = languageRepo.getLanguage(guildId)
        if (!channelPermissionChecker.hasSelfWritePermission(resultchannelParam)) {
            errorSender.sendMessage(K18n_NoWritePermissionInChannel(resultchannelParam))
            return
        }
        var matchUpData: MatchUpData? = null
        var invalidBo3 = false
        val games = mutableListOf<GameData>()
        val allResultMessages: MutableList<ResultMessage> = mutableListOf()
        var finalResultChannel: Long = resultchannelParam
        var shouldntSendData: ShouldntSendData? = null
        for ((urlIndex, singleGame) in fullInputGame.games.withIndex()) {
            val firstReplay = urlIndex == 0
            val source = singleGame.source
            val fromReplay = (source as? GameSource.FromReplay)
            val leaguedata = source.leagueResult
            if (leaguedata != null) run {
                val leagueName = leaguedata.leaguename
                val uindices = leaguedata.uindices
                val (idx1, idx2) = uindices
                val scheduleData = leagueScheduleRepo.getScheduleData(leagueName, idx1, idx2) ?: return@run
                val p1IsSecond = scheduleData.p1IsSecond(idx1)
                val uindicesInOrder = uindices.reversedIf(p1IsSecond)
                val newMatchUpData =
                    MatchUpData(leagueName, scheduleData.week, scheduleData.battleIndex, uindicesInOrder)
                if (!firstReplay && matchUpData != newMatchUpData) {
                    invalidBo3 = true
                    break
                }
                matchUpData = newMatchUpData
                val config = leagueConfigRepo.getConfig(leagueName)
                val configReplayChannel = config.replayChannel.takeIf { customGuild == null }
                val configResultChannel = config.resultChannel.takeIf { customGuild == null }
                val week = scheduleData.week
                val mentions = leagueMemberRepo.getPrimaryIds(leagueName, uindices).mapValues {
                    it.value.joinToTeammates()
                }
                if ((shouldntSendData == null) && ((config.gameDataStore?.hideResults == true)
                            || (!ignoreHideGames && config.hideGames?.weeks?.contains(week) == true))
                ) {
                    shouldntSendData = ShouldntSendData(week, uindicesInOrder.map { mentions[it] ?: "N/A" })
                }
                if (shouldntSendData == null) {
                    if (configReplayChannel != null) replaySender.sendMessage(Constants.CHECKMARK)
                    if (source is GameSource.FromReplay) {
                        val actualReplaySender = configReplayChannel?.let { channel ->
                            MessageSender {
                                channelInterface.sendMessage(channel, it)
                            }
                        } ?: replaySender
                        actualReplaySender.sendMessage(
                            MessageCreate(
                                content = source.url, embeds = Embed {
                                    this.title =
                                        "${source.format} replay: ${source.showdownUserNames.joinToString(" vs. ")}"
                                    this.url = source.url
                                    this.description =
                                        "${K18n_Week.translateTo(language)} $week: " + uindices.withIndex().joinToString(" vs. ") { (index, idx) ->
                                            mentions[idx] ?: source.showdownUserNames[index]
                                        }
                                }.into()
                            )
                        )
                    }
                    if (configResultChannel != null) finalResultChannel = configResultChannel
                    allResultMessages += resultMessageBuilder.getResultMessages(
                        game = singleGame.kd,
                        is4v4 = singleGame.is4v4,
                        language = language,
                        dontTranslateFromReplayServer = fullInputGame.dontTranslatePokemon,
                        playerNames = uindices.mapIndexed { index, idx ->
                            mentions[idx] ?: fromReplay?.let { fromReplay ->
                                fromReplay.showdownUserNames[index]
                            } ?: "N/A"
                        },
                        gid = guildId,
                        defaultNameLookup = singleGame.defaultNameLookup
                    )
                }
                games += GameData(
                    kd = singleGame.kd.reversedIf(p1IsSecond),
                    url = fromReplay?.url ?: "",
                    winnerIndex = singleGame.winnerIndex.let { if (p1IsSecond) 1 - it else it })
            } else if (source is GameSource.FromReplay) {
                if (matchUpData != null) invalidBo3 = true
                fromReplay?.url?.let { url ->
                    replaySender.sendMessage(url)
                }
                allResultMessages += resultMessageBuilder.getResultMessages(
                    game = singleGame.kd,
                    is4v4 = singleGame.is4v4,
                    language = language,
                    dontTranslateFromReplayServer = fullInputGame.dontTranslatePokemon,
                    playerNames = source.showdownUserNames,
                    gid = guildId,
                    defaultNameLookup = singleGame.defaultNameLookup
                )
            } else {
                error("Should not happen, source without league data should be of type Direct with leagueResult null")
            }
        }
        if (invalidBo3) {
            errorSender.sendMessage(K18n_Analysis.InvalidBo3)
            return
        }
        if (shouldntSendData != null) {
            infoSender.sendMessage(K18n_Analysis.BattleSaved)
            channelInterface.sendMessage(
                finalResultChannel, MessageCreate(
                    embeds = Embed(
                        title = "${K18n_Week.translateTo(language)} ${shouldntSendData.week}",
                        description = shouldntSendData.mentions.joinToString(" vs. ") + " ${Constants.CHECKMARK}"
                    ).into()
                )
            )
        } else {
            sendResultMessages(allResultMessages, finalResultChannel, matchUpData?.week, language)
        }
        if (games.isNotEmpty() && matchUpData != null) {
            docEntryService.checkAndProcess(
                matchUpData.leagueName, FullGameData(
                    uindices = matchUpData.uindicesInOrder,
                    week = matchUpData.week,
                    battleIndex = matchUpData.battleIndex,
                    games = games
                ), withSort = withSort
            )
        }
    }

    private suspend fun sendResultMessages(messages: List<ResultMessage>, channelId: Long, week: Int?, language: K18nLanguage) {
        val resultSender = channelInterface.createSingleChannel(channelId)
        for (message in messages) {
            when (message) {
                is ResultMessage.Game -> {
                    resultSender.sendMessage(
                        MessageCreate(
                            embeds = Embed(
                                description = message.description,
                                authorName = K18n_UpdateNotice.translateTo(language),
                                authorUrl = "${botConstants.webBaseUrl}/${language.name.lowercase()}/update",
                                title = week?.let { "${K18n_Week.translateTo(language)} $it" }
                            ).into()
                        )
                    )
                }

                is ResultMessage.IllusionWarning -> {
                    resultSender.sendMessage(
                        K18n_Analysis.IllusionWarning(message.playerName).translateTo(language)
                    )
                }

                is ResultMessage.KillsDeathsNotMatching -> {
                    resultSender.sendMessage(
                        K18n_Analysis.KillsDeathsNotMatching(
                            (if (message.illusion) K18n_Analysis.PotentialZoroark else K18n_Analysis.OtherIssue).translateTo(
                                language
                            )
                        ).translateTo(language)
                    )
                }
            }
        }
    }

    private fun <T> Iterable<T>.reversedIf(condition: Boolean) = if (condition) reversed() else this.toList()

    private data class ShouldntSendData(val week: Int, val mentions: List<String>)

    private data class MatchUpData(
        val leagueName: String, val week: Int, val battleIndex: Int, val uindicesInOrder: List<Int>
    )
}
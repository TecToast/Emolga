package de.tectoast.emolga.domain.league.draft.service.execution.display

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.editMessage
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.execution.DraftActionResult
import de.tectoast.emolga.domain.league.draft.model.execution.DraftExecution
import de.tectoast.emolga.domain.league.draft.model.execution.PreparedDraftLogEntry
import de.tectoast.emolga.domain.league.draft.model.execution.TimerOption
import de.tectoast.emolga.domain.league.draft.repository.DraftLastAnnounceRepository
import de.tectoast.emolga.domain.league.draft.repository.DraftLogMessageIdRepository
import de.tectoast.emolga.domain.league.draft.repository.DraftLogRepository
import de.tectoast.emolga.domain.league.draft.service.execution.display.message.DraftLogEntryMessageDispatcher
import de.tectoast.emolga.domain.league.draft.util.DisplayHelperFactory
import de.tectoast.emolga.domain.league.draft.util.getDisplayName
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.league.K18n_League
import de.tectoast.generic.K18n_MadeUpFor
import de.tectoast.generic.K18n_Round
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent

@Single
class CombinedLogsDraftDisplayService(
    private val channelInterface: ChannelInterface,
    private val displayHelperFactory: DisplayHelperFactory,
    private val draftLogRepository: DraftLogRepository,
    private val draftLogMessageIdRepository: DraftLogMessageIdRepository,
    private val draftLastAnnounceRepository: DraftLastAnnounceRepository,
    private val leagueMemberRepository: LeagueMemberRepository,
    private val pokemonDisplayService: PokemonDisplayService,
    private val draftLogEntryMessageDispatcher: DraftLogEntryMessageDispatcher,
    baseScope: CoroutineScope
) : DraftDisplayService, KoinComponent {
    val scope = baseScope + CoroutineName("DraftDisplayService")
    override suspend fun handleDraftExecution(
        ctx: DraftRunContext,
        execution: DraftExecution,
        modifiedRounds: Set<Int>,
        language: K18nLanguage
    ) {
        val displayHelper = displayHelperFactory.create(ctx)
        val draftChannel = ctx.league.draftChannel
        var isDraftFinished = false
        val currentRound = ctx.league.round
        val session = ctx.league.draftData.draftSessionNum
        val leagueName = ctx.league.leagueName
        val logEntriesForRounds = draftLogRepository.getLogEntriesForRounds(
            leagueName, session, modifiedRounds
        )
        val messageIds = draftLogMessageIdRepository.getMessageIds(leagueName, session, modifiedRounds)
        val primaryIds = leagueMemberRepository.getPrimaryIds(leagueName)
        val roundBase = K18n_Round.translateTo(language)
        val pokemonDisplayFn: suspend (ShowdownID) -> String = { pokemonDisplayService.getDisplayName(it, ctx, withAdditionalEnglish = true) }
        val maxRound = ctx.league.draftOrder.size
        logEntriesForRounds.entries.sortedBy { it.key }.forEach { (round, logEntries) ->
            val logContent =
                logEntries.map { it.toMessage(primaryIds, language, pokemonDisplayFn) }.joinToString("\n") {
                    "- $it"
                }
            val roundMessage = if(round <= maxRound) "$roundBase $round" else K18n_MadeUpFor.translateTo(language)
            val fullMessage = "# $roundMessage\n$logContent"
            val messageId = messageIds[round]
            if (messageId != null) {
                channelInterface.editMessage(draftChannel, messageId, fullMessage)
            } else {
                channelInterface.sendMessage(draftChannel, fullMessage)?.let {
                    draftLogMessageIdRepository.setMessageId(leagueName, session, round, it)
                }
            }
        }

        if (currentRound !in logEntriesForRounds && currentRound <= maxRound) {
            channelInterface.sendMessage(draftChannel, "# $roundBase $currentRound")?.let {
                draftLogMessageIdRepository.setMessageId(leagueName, session, currentRound, it)
            }
        }
        execution.results.forEach { result ->
            result.deletesMessage.forEach {
                channelInterface.deleteMessage(draftChannel, it)
            }
            result.sendsMessage.forEach { messageSend ->
                channelInterface.sendMessage(draftChannel, messageSend(displayHelper).translateTo(language))
            }
            if (result is DraftActionResult.DraftFinished) {
                isDraftFinished = true
            }
        }
        if (isDraftFinished || execution.timerOption == TimerOption.CANCEL) {
            deleteLastAnnounceMessage(leagueName, session, draftChannel)
        } else {
            scope.launch {
                execution.idxToAnnounce?.let { idx ->
                    deleteLastAnnounceMessage(leagueName, session, draftChannel)
                    val announceData = displayHelper.buildAnnounceData(idx, withTimerAnnounce = true)
                    val currentMention = displayHelper.getPingForUser(idx)
                    channelInterface.sendMessage(
                        draftChannel,
                        "-------------------------------\n" + K18n_League.AnnouncePlayer(
                            currentMention.content,
                            announceData.translateTo(language)
                        )
                            .translateTo(language),
                        mentionUsers = currentMention.enabledMentions
                    )?.let {
                        draftLastAnnounceRepository.setLastAnnounceId(leagueName, session, it)
                    }
                }
            }
        }
    }

    private suspend fun deleteLastAnnounceMessage(leagueName: String, session: Int, draftChannel: Long) {
        draftLastAnnounceRepository.deleteAndGetLastAnnounceId(leagueName, session)?.let {
            channelInterface.deleteMessage(draftChannel, it)
        }
    }

    private suspend fun PreparedDraftLogEntry.toMessage(
        idxToIds: Map<Int, List<Long>>,
        language: K18nLanguage,
        pokemonDisplayFn: suspend (ShowdownID) -> String
    ): String {
        val pingPart = idxToIds[idx]?.joinToString { "<@${it}>" } ?: "N/A"
        return draftLogEntryMessageDispatcher.createMessage(entry, pingPart, pokemonDisplayFn).translateTo(language)
    }
}

package de.tectoast.emolga.domain.league.draft.service.execution.display

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.editMessage
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.draft.model.execution.DraftExecution
import de.tectoast.emolga.domain.league.draft.model.execution.PreparedDraftLogEntry
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
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.generic.K18n_Round
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
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
    dispatcher: CoroutineDispatcher
) : DraftDisplayService, KoinComponent {
    val scope = createCoroutineScope("DraftDisplayService", dispatcher)
    override suspend fun handleDraftExecution(
        ctx: DraftRunContext,
        execution: DraftExecution,
        modifiedRounds: Set<Int>,
        language: K18nLanguage
    ) {
        val displayHelper = displayHelperFactory.create(ctx)
        val draftChannel = ctx.league.draftChannel
        execution.results.forEach { result ->
            result.editsMessage.forEach { messageEdit ->
                channelInterface.editMessage(
                    draftChannel,
                    messageEdit.first,
                    messageEdit.second(displayHelper).translateTo(language)
                )
            }
            result.sendsMessage.forEach { messageSend ->
                channelInterface.sendMessage(draftChannel, messageSend(displayHelper).translateTo(language))
            }
        }
        val currentRound = ctx.league.round
        val session = ctx.league.draftData.draftSessionNum
        val leagueName = ctx.league.leagueName
        val logEntriesForRounds = draftLogRepository.getLogEntriesForRounds(
            leagueName, session, modifiedRounds
        )
        val messageIds = draftLogMessageIdRepository.getMessageIds(leagueName, session, modifiedRounds)
        val primaryIds = leagueMemberRepository.getPrimaryIds(leagueName)
        val roundBase = K18n_Round.translateTo(language)
        val pokemonDisplayFn: suspend (ShowdownID) -> String = { pokemonDisplayService.getDisplayName(it, ctx) }
        logEntriesForRounds.entries.sortedBy { it.key }.forEach { (round, logEntries) ->
            val logContent = logEntries.map { it.toMessage(primaryIds, language, pokemonDisplayFn) }.joinToString("\n")
            val fullMessage = "# $roundBase $round\n$logContent"
            val messageId = messageIds[round]
            if (messageId != null) {
                channelInterface.editMessage(draftChannel, messageId, fullMessage)
            } else {
                channelInterface.sendMessage(draftChannel, fullMessage)?.let {
                    draftLogMessageIdRepository.setMessageId(leagueName, session, round, it)
                }
            }
        }
        if (currentRound !in logEntriesForRounds) {
            channelInterface.sendMessage(draftChannel, "# $roundBase $currentRound")?.let {
                draftLogMessageIdRepository.setMessageId(leagueName, session, currentRound, it)
            }
        }
        scope.launch {
            execution.idxToAnnounce?.let { idx ->
                draftLastAnnounceRepository.getLastAnnounceId(leagueName, session)?.let {
                    channelInterface.deleteMessage(draftChannel, it)
                }
                val announceData = displayHelper.buildAnnounceData(idx, withTimerAnnounce = true)
                val currentMention = displayHelper.getPingForUser(idx)
                channelInterface.sendMessage(draftChannel,
                    "-------------------------------\n" + K18n_League.AnnouncePlayer(currentMention.content, announceData.translateTo(language))
                        .translateTo(language),
                    mentionUsers = currentMention.enabledMentions
                )?.let {
                    draftLastAnnounceRepository.setLastAnnounceId(leagueName, session, it)
                }
            }
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

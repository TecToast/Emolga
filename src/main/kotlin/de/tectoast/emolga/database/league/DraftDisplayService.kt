package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.DraftLogMessageIdService
import de.tectoast.emolga.database.exposed.DraftLogRepository
import de.tectoast.generic.K18n_Round
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.editMessage
import net.dv8tion.jda.api.JDA
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent

interface DraftDisplayService {
    suspend fun handleDraftExecution(
        ctx: DraftRunContext,
        execution: DraftExecution,
        modifiedRounds: Set<Int>,
        language: K18nLanguage
    )
}

@Single
class JDADraftDisplayService(
    val jda: JDA,
    val displayHelperFactory: DisplayHelperFactory,
    val draftLogRepository: DraftLogRepository,
    val draftLogMessageIdService: DraftLogMessageIdService,
    val leagueMemberRepository: LeagueMemberRepository
) : DraftDisplayService, KoinComponent {
    override suspend fun handleDraftExecution(
        ctx: DraftRunContext,
        execution: DraftExecution,
        modifiedRounds: Set<Int>,
        language: K18nLanguage
    ) {
        val displayHelper = displayHelperFactory.create(ctx)
        val tc = jda.getTextChannelById(ctx.league.draftChannel) ?: return
        execution.results.forEach { result ->
            result.editsMessage.forEach { messageEdit ->
                tc.editMessage(messageEdit.first.toString(), messageEdit.second(displayHelper).translateTo(language))
                    .queue()
            }
            result.sendsMessage.forEach { messageSend ->
                tc.sendMessage(messageSend(displayHelper).translateTo(language)).queue()
            }
        }
        val session = ctx.league.draftData.draftSessionNum
        val leagueName = ctx.league.leagueName
        val logEntriesForRounds = draftLogRepository.getLogEntriesForRounds(
            leagueName, session, modifiedRounds
        )
        val messageIds = draftLogMessageIdService.getMessageIds(leagueName, session, modifiedRounds)
        val primaryIds = leagueMemberRepository.getPrimaryIds(leagueName)
        val roundBase = K18n_Round.translateTo(language)
        logEntriesForRounds.forEach { (round, logEntries) ->
            val logContent = logEntries.joinToString("\n") { it.toMessage(primaryIds, language) }
            val fullMessage = "$roundBase $round:\n$logContent"
            val messageId = messageIds[round]
            if (messageId != null) {
                tc.editMessage(messageId.toString(), fullMessage).queue()
            } else {
                val resultingMessageId = tc.sendMessage(fullMessage).await().idLong
                draftLogMessageIdService.setMessageId(leagueName, session, round, resultingMessageId)
            }
        }
        execution.idxToAnnounce?.let { idx ->
            val announceData = displayHelper.buildAnnounceData(idx, withTimerAnnounce = true)
            tc.sendMessage(announceData.translateTo(language)).queue()
        }
    }

    private fun PreparedDraftLogEntry.toMessage(idxToIds: Map<Int, List<Long>>, language: K18nLanguage): String {
        val pingPart = idxToIds[idx]?.joinToString { "<@${it}>" } ?: "N/A"
        return entry.toMessageContent(pingPart).translateTo(language)
    }
}

@Single
class DisplayHelperFactory(
    val announceService: DraftAnnounceService,
    val picksRepo: LeaguePickRepository,
    val leagueMentionService: LeagueMentionService
) {
    fun create(ctx: DraftRunContext): DisplayHelper =
        MainDisplayHelper(announceService, picksRepo, leagueMentionService, ctx)
}


private class MainDisplayHelper(
    val announceService: DraftAnnounceService,
    val picksRepo: LeaguePickRepository,
    val leagueMentionService: LeagueMentionService,
    val ctx: DraftRunContext,
) : DisplayHelper {
    override suspend fun buildAnnounceData(idx: Int, withTimerAnnounce: Boolean): K18nMessage {
        return announceService.generateAnnounceData(
            idx, picksRepo.getPicksForUser(ctx.league.leagueName, idx), withTimerAnnounce, ctx, ctx.tierlistMeta
        )
    }

    override suspend fun getPingForUser(idx: Int) =
        leagueMentionService.getMentionForParticipant(ctx.league.leagueName, idx)
}

package de.tectoast.emolga.domain.league.draft.service.util

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.queue.repository.QueuedPicksRepository
import de.tectoast.emolga.utils.joinToTeammates
import kotlinx.coroutines.*
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class SuccessfulQueueNotificationService(
    private val queuedPicksRepo: QueuedPicksRepository,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val channelInterface: ChannelInterface,
    baseScope: CoroutineScope,
) {
    private val scope = baseScope + CoroutineName("SuccessfulQueueNotificationService")
    fun notifySuccessfulQueue(ctx: DraftRunContext, uindices: Set<Int>) = scope.launch {
        val leagueName = ctx.league.leagueName
        val queuedData = queuedPicksRepo.getForLeague(leagueName)
        val indicesToNotify =
            queuedData.entries.filter { it.key in uindices && it.value.notifyOnSuccess }.map { it.key }
        if (indicesToNotify.isEmpty()) return@launch
        val primaryIds = leagueMemberRepo.getPrimaryIds(leagueName, indicesToNotify)
        val message = indicesToNotify.joinToString { primaryIds[it]?.joinToTeammates() ?: "N/A" }
        val channelId = ctx.league.draftChannel
        val messageId = channelInterface.sendMessage(channelId, message)
        messageId?.let {
            withContext(NonCancellable) {
                delay(1.seconds)
                channelInterface.deleteMessage(channelId, messageId)
            }
        }
    }
}

package de.tectoast.emolga.domain.league.draft.service.timer

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.member.service.LeagueMentionService
import de.tectoast.emolga.league.K18n_League
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.*
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single
class DraftStallSecondService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueMentionService: LeagueMentionService,
    private val timerService: DraftTimerService,
    private val channelSender: ChannelInterface,
    private val languageRepo: GuildConfigRepository,
    baseScope: CoroutineScope
) : StartupTask {
    private val scope = baseScope + CoroutineName("DraftStallSecondService")
    override suspend fun onStartup() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            timerService.expiredStallSecondEvents.collect { leagueName ->
                val leagueData = leagueCoreRepo.getDraftRelevantData(leagueName) ?: return@collect
                val timerRelated = leagueData.draftData.timer
                timerRelated.lastStallSecondUsedMid =
                    sendStallSecondMessage(
                        leagueData.draftChannel,
                        leagueName,
                        leagueData.currentIdx,
                        timerRelated.cooldown,
                        languageRepo.getLanguage(leagueData.guild)
                    )
                leagueCoreRepo.updateDraftData(leagueName, leagueData.draftData)
            }
        }
    }

    private suspend fun sendStallSecondMessage(
        channelId: Long,
        leagueName: String,
        idx: Int,
        cooldown: Instant,
        language: K18nLanguage
    ): Long {
        val mentionData = leagueMentionService.getMentionForParticipant(leagueName, idx)
        return channelSender.sendMessage(
            channelId,
            K18n_League.StallSecondsRunning(mentionData.content, cooldown.epochSeconds).translateTo(language),
            mentionData.enabledMentions
        )!!
    }
}

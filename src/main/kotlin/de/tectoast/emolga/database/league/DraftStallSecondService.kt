package de.tectoast.emolga.database.league

import de.tectoast.emolga.database.exposed.GuildLanguageRepository
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.league.K18n_League
import de.tectoast.emolga.utils.ChannelMessageSender
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.sendMessage
import de.tectoast.k18n.generated.K18nLanguage
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

class DraftStallSecondService(
    val leagueCoreRepo: LeagueCoreRepository,
    val leagueMentionService: LeagueMentionService,
    val timerService: DraftTimerService,
    val channelSender: ChannelMessageSender,
    val languageRepo: GuildLanguageRepository,
    dispatcher: CoroutineDispatcher
) : StartupTask {
    private val scope = createCoroutineScope("DraftStallSecondService", dispatcher)
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
        cooldown: Long,
        language: K18nLanguage
    ): Long {
        val mentionData = leagueMentionService.getMentionForParticipant(leagueName, idx)
        return channelSender.sendMessage(
            channelId,
            K18n_League.StallSecondsRunning(mentionData.content, cooldown / 1000).translateTo(language),
             mentionData.enabledMentions
        )!!
    }
}

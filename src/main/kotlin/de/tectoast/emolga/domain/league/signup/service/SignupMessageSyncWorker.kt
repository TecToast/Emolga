package de.tectoast.emolga.domain.league.signup.service

import de.tectoast.emolga.di.DiscordReadyTask
import de.tectoast.emolga.di.TransactionRunner
import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.editMessage
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.signup.model.LeagueSignupConfig
import de.tectoast.emolga.domain.league.signup.repository.SignupRepository
import de.tectoast.emolga.features.league.K18n_Signup
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class SignupMessageSyncWorker(
    private val signupRepo: SignupRepository,
    private val channelInterface: ChannelInterface,
    private val tx: TransactionRunner,
    private val languageRepo: GuildConfigRepository,
    baseScope: CoroutineScope
) : DiscordReadyTask {
    private val scope = baseScope + CoroutineName("SignupMessageSyncWorker")
    private val wakeupSignal = Channel<Unit>(Channel.CONFLATED)
    override suspend fun onDiscordReady() {
        scope.launch {
            syncDirtySignups()
            while (isActive) {
                wakeupSignal.receive()
                delay(2.seconds)
                syncDirtySignups()
                delay(8.seconds)
            }
        }
    }

    private suspend fun syncDirtySignups() = tx {
        val dirtySignups = signupRepo.getDirtySignups()
        dirtySignups.forEach { signup ->
            updateSignupMessage(
                signup.config,
                signup.announceMessageId,
                signup.userCount,
                languageRepo.getLanguage(signup.guild)
            )
        }
        signupRepo.setNewDocumentedCount(dirtySignups.associate { it.id to it.userCount })
    }

    fun notifySignupChange() {
        wakeupSignal.trySend(Unit)
    }

    private suspend fun updateSignupMessage(
        config: LeagueSignupConfig,
        announceMessageId: Long,
        userCount: Long,
        language: K18nLanguage,
        setMaxUsersToCurrentUsers: Boolean = false
    ) {
        channelInterface.editMessage(config.announceChannel, announceMessageId, buildString {
            append(config.signupMessage)
            if (!config.hideUserCount) {
                val current = userCount.toString()
                append("\n\n")
                append(
                    K18n_Signup.SignupMessageData(
                        current,
                        if (setMaxUsersToCurrentUsers) current else config.maxUsersAsString
                    ).translateTo(language)
                )
            }
        })

    }
}
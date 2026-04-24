package de.tectoast.emolga.database.league

import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.k18n.generated.K18nLanguage
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.koin.core.annotation.Single

@Single
class SnipeNotificationService(val dmSender: DMSender, val leagueMemberRepository: LeagueMemberRepository) {

    suspend fun handleDraftSnipes(
        leagueName: String,
        displayName: String,
        snipeMap: Map<Int, SnipeMeta>,
        language: K18nLanguage
    ) {
        val idsToPing = leagueMemberRepository.getAllIdsToPing(leagueName)
        snipeMap.entries.forEach { (idx, snipes) ->
            idsToPing[idx]?.forEach { notifySnipes(it, displayName, snipes, language) }
        }
    }

    private fun notifySnipes(userId: Long, leagueDisplayName: String, snipes: SnipeMeta, language: K18nLanguage) {
        val snipeMessage = snipes.list.joinToString("\n") {
            it.pokemon
        }
        val description = (if (snipes.disableIfSniped) {
            K18n_QueuePicks.SnipeWarningDisabled(
                snipeMessage
            )
        } else K18n_QueuePicks.SnipeWarningEnabled(
            snipeMessage
        )).translateTo(language)
        val message = MessageCreate(
            embeds = Embed(
                title = "Queue - $leagueDisplayName", color = 0xFF0000, description = description
            ).into()
        )
        dmSender.sendDM(userId, message)
    }

}


interface DMSender {
    fun sendDM(userId: Long, message: MessageCreateData)
}

@Single
class JDADMSender(val jda: JDA) : DMSender {
    override fun sendDM(userId: Long, message: MessageCreateData) {
        jda.openPrivateChannelById(userId).flatMap { it.sendMessage(message) }.queue()
    }
}
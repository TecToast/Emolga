package de.tectoast.emolga.domain.league.signup.service.logo.settings

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.editMessage
import de.tectoast.emolga.domain.language.repository.GuildLanguageRepository
import de.tectoast.emolga.domain.league.signup.model.LeagueSignup
import de.tectoast.emolga.domain.league.signup.model.LogoInputData
import de.tectoast.emolga.domain.league.signup.model.LogoSettings
import de.tectoast.emolga.domain.league.signup.model.SignupEntry
import de.tectoast.emolga.utils.joinToTeammates
import de.tectoast.emolga.utils.json.K18n_SignupInput
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.into
import org.koin.core.annotation.Single

@Single
class ChannelLogoSettingsHandler(
    private val channelInterface: ChannelInterface,
    private val languageRepo: GuildLanguageRepository
) :
    LogoSettingsHandler<LogoSettings.Channel> {
    override val targetClass = LogoSettings.Channel::class

    private fun getMsgTitle(data: SignupEntry): K18nMessage =
        K18n_SignupInput.LogoMsg(data.users.joinToTeammates(), data.data["teamname"]?.let { " ($it)" }.orEmpty())

    override suspend fun handleLogo(
        config: LogoSettings.Channel, leagueSignup: LeagueSignup, data: SignupEntry, logoData: LogoInputData
    ): Long? {
        val fileUpload = logoData.toFileUpload()
        val language = languageRepo.getLanguage(leagueSignup.guild)
        data.logoMessageId?.let { messageId ->
            channelInterface.editMessage(
                config.channelId,
                messageId,
                MessageEdit(content = getMsgTitle(data).translateTo(language), files = fileUpload.into())
            )
        } ?: run {
            return channelInterface.sendMessage(
                config.channelId,
                MessageCreate(content = getMsgTitle(data).translateTo(language), files = fileUpload.into())
            )
        }
        return null
    }

    override suspend fun handleSignupChange(
        config: LogoSettings.Channel, leagueSignup: LeagueSignup, data: SignupEntry
    ) {
        data.logoMessageId?.let { messageId ->
            channelInterface.editMessage(
                config.channelId, messageId, getMsgTitle(data).translateTo(languageRepo.getLanguage(leagueSignup.guild))
            )
        }
    }

    override suspend fun handleSignupRemoved(
        config: LogoSettings.Channel, leagueSignup: LeagueSignup, data: SignupEntry
    ) {
        data.logoMessageId?.let { messageId -> channelInterface.deleteMessage(config.channelId, messageId) }
    }
}
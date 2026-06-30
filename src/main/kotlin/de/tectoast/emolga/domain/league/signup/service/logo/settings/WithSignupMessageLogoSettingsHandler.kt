package de.tectoast.emolga.domain.league.signup.service.logo.settings

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.league.signup.model.LeagueSignup
import de.tectoast.emolga.domain.league.signup.model.LogoInputData
import de.tectoast.emolga.domain.league.signup.model.LogoSettings
import de.tectoast.emolga.domain.league.signup.model.SignupEntry
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.into
import org.koin.core.annotation.Single

@Single
class WithSignupMessageLogoSettingsHandler(val channelInterface: ChannelInterface) :
    LogoSettingsHandler<LogoSettings.WithSignupMessage> {
    override val targetClass = LogoSettings.WithSignupMessage::class

    override suspend fun handleLogo(
        config: LogoSettings.WithSignupMessage, leagueSignup: LeagueSignup, data: SignupEntry, logoData: LogoInputData
    ): Long? {
        val messageId = data.signupMessageId ?: return null
        channelInterface.editMessage(
            leagueSignup.config.signupChannel, messageId, MessageEdit(files = logoData.toFileUpload().into())
        )
        return null
    }
}
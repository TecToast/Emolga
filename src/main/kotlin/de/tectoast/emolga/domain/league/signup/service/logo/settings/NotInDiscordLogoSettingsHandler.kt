package de.tectoast.emolga.domain.league.signup.service.logo.settings

import de.tectoast.emolga.domain.league.signup.model.LogoSettings
import org.koin.core.annotation.Single


@Single
class NotInDiscordHandler : LogoSettingsHandler<LogoSettings.NotInDiscord> {
    override val targetClass = LogoSettings.NotInDiscord::class
}

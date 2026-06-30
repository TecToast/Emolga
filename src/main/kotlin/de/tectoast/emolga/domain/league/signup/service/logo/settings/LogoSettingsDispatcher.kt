package de.tectoast.emolga.domain.league.signup.service.logo.settings

import de.tectoast.emolga.domain.league.signup.model.LeagueSignup
import de.tectoast.emolga.domain.league.signup.model.LogoInputData
import de.tectoast.emolga.domain.league.signup.model.LogoSettings
import de.tectoast.emolga.domain.league.signup.model.SignupEntry
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class LogoSettingsDispatcher(handlers: List<LogoSettingsHandler<LogoSettings>>) : LogoSettingsOperations<LogoSettings> {
    private val registry = HandlerRegistry(handlers)

    override suspend fun handleLogo(
        config: LogoSettings, leagueSignup: LeagueSignup, data: SignupEntry, logoData: LogoInputData
    ) = registry.getHandler(config).handleLogo(config, leagueSignup, data, logoData)

    override suspend fun handleSignupChange(
        config: LogoSettings, leagueSignup: LeagueSignup, data: SignupEntry
    ) = registry.getHandler(config).handleSignupChange(config, leagueSignup, data)

    override suspend fun handleSignupRemoved(
        config: LogoSettings, leagueSignup: LeagueSignup, data: SignupEntry
    ) = registry.getHandler(config).handleSignupRemoved(config, leagueSignup, data)
}
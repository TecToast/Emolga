package de.tectoast.emolga.domain.league.signup.service.logo.settings

import de.tectoast.emolga.domain.league.signup.model.LeagueSignup
import de.tectoast.emolga.domain.league.signup.model.LogoInputData
import de.tectoast.emolga.domain.league.signup.model.LogoSettings
import de.tectoast.emolga.domain.league.signup.model.SignupEntry
import de.tectoast.emolga.utils.handler.BaseHandler

interface LogoSettingsOperations<C : LogoSettings> {
    suspend fun handleLogo(config: C, leagueSignup: LeagueSignup, data: SignupEntry, logoData: LogoInputData): Long?
    suspend fun handleSignupChange(config: C, leagueSignup: LeagueSignup, data: SignupEntry)
    suspend fun handleSignupRemoved(config: C, leagueSignup: LeagueSignup, data: SignupEntry)
}

interface LogoSettingsHandler<C : LogoSettings> : BaseHandler<C>, LogoSettingsOperations<C> {
    override suspend fun handleLogo(
        config: C, leagueSignup: LeagueSignup, data: SignupEntry, logoData: LogoInputData
    ): Long? = null

    override suspend fun handleSignupChange(
        config: C, leagueSignup: LeagueSignup, data: SignupEntry
    ) {
    }

    override suspend fun handleSignupRemoved(
        config: C, leagueSignup: LeagueSignup, data: SignupEntry
    ) {
    }
}
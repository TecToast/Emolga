package de.tectoast.emolga.domain.web.service

import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import de.tectoast.emolga.domain.web.repository.LoginCodeRepository
import de.tectoast.emolga.features.various.K18n_Login
import de.tectoast.emolga.ktor.controllers.emolga.authenticated.DiscordUserSession
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.k18n.generated.K18nMessage
import org.koin.core.annotation.Single

@Single
class LoginService(
    private val botConstants: BotConstants,
    private val loginCodeRepository: LoginCodeRepository,
    private val discordUserServer: DiscordUserService
) {
    suspend fun createLogin(userId: Long, guildId: Long): K18nMessage {
        return K18n_Login.Success(botConstants.webBaseUrl, loginCodeRepository.createLoginCode(userId, guildId))
    }

    suspend fun loginFromWeb(code: String): DiscordUserSession? {
        val (userId, guildId) = loginCodeRepository.getUserAndGuildByCode(code) ?: return null
        loginCodeRepository.removeLoginCode(code)
        val userData = discordUserServer.getData(guildId, listOf(userId))[userId] ?: return null
        return DiscordUserSession(
            userData.userId,
            userData.displayName,
            userData.avatarUrl.substringAfter("/${userData.userId}/").substringBefore(".")
        )
    }
}
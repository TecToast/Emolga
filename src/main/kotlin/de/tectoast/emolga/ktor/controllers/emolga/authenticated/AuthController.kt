package de.tectoast.emolga.ktor.controllers.emolga.authenticated

import de.tectoast.emolga.domain.league.admin.repository.GuildManagerRepository
import de.tectoast.emolga.domain.web.repository.DiscordUserSessionRepository
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.utils.BotConfig
import de.tectoast.emolga.utils.BotConstants
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class AuthController(
    private val httpClient: HttpClient,
    private val oauthConfig: BotConfig.Oauth2,
    private val botConstants: BotConstants,
    private val sessionStorage: RepositorySessionStorage,
    private val guildManagerRepo: GuildManagerRepository
) : WebController("/") {

    override fun Route.setup() {
        authenticate("auth-oauth-discord") {
            get("/api/login") {}
            get("/${oauthConfig.callbackUrl.substringAfter("://").substringAfter("/")}") {
                val principal: OAuthAccessTokenResponse.OAuth2 =
                    call.principal() ?: return@get call.respondRedirect("/")
                val accessToken = principal.accessToken
                val user = httpClient.getLoggedInUser(accessToken)
                val userId = user.id.toLong()
                if (!guildManagerRepo.isUserAuthorized(userId)) {
                    return@get call.respondRedirect("/alpha")
                }
                call.sessions.set(DiscordUserSession(userId, user.displayName, user.avatar))
                call.respondRedirect("/")
            }
        }
        get("/api/logout") {
            call.sessions.clear<DiscordUserSession>()
            call.respondRedirect("/")
        }
        if (oauthConfig.devMode) {
            get("/api/dev") {
                call.sessions.set(DiscordUserSession(botConstants.botOwnerId, "DEV", ""))
                call.respondRedirect("/")
            }
        }
    }

    override fun Application.setupApplication() {
        install(Sessions) {
            cookie<DiscordUserSession>("emolga_auth", storage = sessionStorage) {
                cookie.extensions["SameSite"] = "lax"
            }
        }
        authentication {
            oauth("auth-oauth-discord") {
                urlProvider = { oauthConfig.callbackUrl }
                settings = OAuthServerSettings.OAuth2ServerSettings(
                    name = "discord",
                    authorizeUrl = "https://discord.com/api/oauth2/authorize",
                    accessTokenUrl = "https://discord.com/api/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = oauthConfig.clientid,
                    clientSecret = oauthConfig.clientsecret,
                    defaultScopes = listOf("identify"),
                    extraAuthParameters = listOf("grant_type" to "authorization_code"),
                    nonceManager = StatelessHmacNonceManager(
                        key = oauthConfig.nonceKey.encodeToByteArray(),
                        timeoutMillis = 120000
                    )
                )
                client = httpClient
            }
        }
    }
}

private suspend fun HttpClient.getLoggedInUser(accessToken: String) =
    get(
        "https://discord.com/api/v10/users/@me",
    ) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }.body<LoggedInUser>()


@Serializable
private data class LoggedInUser(val id: String, @SerialName("global_name") val displayName: String, val avatar: String)

@Serializable
data class DiscordUserSession(val userId: Long, val displayName: String, val avatar: String)

@Single
class RepositorySessionStorage(private val repository: DiscordUserSessionRepository) : SessionStorage {
    override suspend fun write(id: String, value: String) = repository.set(id, value)
    override suspend fun read(id: String): String = repository.get(id)
    override suspend fun invalidate(id: String) = repository.delete(id)
}

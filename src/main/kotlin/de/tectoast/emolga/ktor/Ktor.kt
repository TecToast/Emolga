package de.tectoast.emolga.ktor

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.httpClient
import de.tectoast.emolga.commands.webJSON
import de.tectoast.emolga.database.exposed.DiscordAuth
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.User
import org.slf4j.event.Level
import kotlin.collections.set

object Ktor {

    fun start() {
        embeddedServer(Netty, applicationEngineEnvironment {
            /*val (keystoreFile, password, keyAlias) = Command.tokens.website.let {
                Triple(File(it.path), it.password, it.keyalias)
            }
            val keystore = KeyStore.getInstance("JKS").apply {
                load(keystoreFile.inputStream(), password.toCharArray())
            }
            sslConnector(
                keyStore = keystore,
                keyAlias = keyAlias,
                keyStorePassword = { password.toCharArray() },
                privateKeyPassword = { password.toCharArray() }) {
                port = 51216
                keyStorePath = keystoreFile
            }*/
            connector {
                port = 51216
            }
            module {
                install(Sessions) {
                    cookie<UserSession>("user_session", DiscordAuth) {
                        cookie.extensions["SameSite"] = "None"
                        cookie.httpOnly = true
                    }
                }
                install(ContentNegotiation) {
                    json(webJSON)
                }
                install(CORS) {
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Put)
                    allowMethod(HttpMethod.Delete)
                    allowMethod(HttpMethod.Patch)
                    allowHeader(HttpHeaders.Authorization)
                    allowHost("emolga.tectoast.de", schemes = listOf("https"))
                    allowHost("localhost:4200")
                    allowCredentials = true
                }
                install(CallLogging) {
                    level = Level.INFO
                }
                authentication {
                    oauth("auth-oauth-discord") {
                        urlProvider = { "https://emolga.tectoast.de/api/discordauth" }
                        providerLookup = {
                            OAuthServerSettings.OAuth2ServerSettings(
                                name = "discord",
                                authorizeUrl = "https://discord.com/api/oauth2/authorize?client_id=723829878755164202&redirect_uri=https%3A%2F%2Femolga.tectoast.de%2Fapi%2Fdiscordauth&response_type=code&scope=identify%20guilds",
                                accessTokenUrl = "https://discord.com/api/oauth2/token",
                                requestMethod = HttpMethod.Post,
                                clientId = "723829878755164202",
                                clientSecret = Command.tokens.oauth2.clientsecret,
                                defaultScopes = listOf("identify", "guilds"),
                                extraAuthParameters = listOf("grant_type" to "authorization_code")
                            )
                        }
                        client = httpClient
                    }
                }
                routing {
                    route("/api") {
                        emolgaAPI()
                        ytSubscribtions()
                    }
                }
            }
        }).start()
    }
}


fun ApplicationCall.sessionOrUnauthorized(): UserSession? {
    return sessions.get() ?: run {
        response.status(HttpStatusCode.Unauthorized)
        null
    }
}

@Serializable
data class UserSession(val accessToken: String, val refreshToken: String, val expires: Long, val userId: Long)

@Serializable
data class DiscordUser(
    val id: Long,
    val username: String,
    val discriminator: Int,
    val avatar: String?
) {
    fun emolga(): DiscordUser = this.copy(avatar = avatar?.let {
        "https://cdn.discordapp.com/avatars/${id}/${
            it + if (it.startsWith("a_")) ".gif" else ".png"
        }"
    } ?: String.format(User.DEFAULT_AVATAR_URL, discriminator % 5)
    )
}

@Serializable
data class DiscordGuildData(val id: String, val name: String, val icon: String?, val permissions: Long)


@Serializable
data class GuildData(val name: String, val url: String, val id: String, val joined: Boolean)

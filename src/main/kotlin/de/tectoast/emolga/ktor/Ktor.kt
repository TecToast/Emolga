package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.httpClient
import de.tectoast.emolga.commands.webJSON
import de.tectoast.emolga.database.exposed.DiscordAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import org.slf4j.event.Level

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
                    authenticate("auth-oauth-discord") {
                        get("/api/login") {}
                        get("/api/discordauth") {
                            val principal: OAuthAccessTokenResponse.OAuth2 = call.principal() ?: run {
                                call.response.status(HttpStatusCode.BadRequest)
                                return@get
                            }
                            val accessToken = principal.accessToken
                            val user = httpClient.getUserData(accessToken)
                            call.sessions.set(
                                UserSession(
                                    accessToken,
                                    principal.refreshToken!!,
                                    principal.expiresIn,
                                    user.id
                                )
                            )
                            call.respondRedirect("https://emolga.tectoast.de/")
                        }
                    }
                    route("/api") {
                        get("/test") {
                            call.respondText("Hello World!")
                        }
                        get("/userdata") {
                            val session = call.sessionOrUnauthorized() ?: return@get
                            call.respond(httpClient.getUserData(session.accessToken).emolga())
                        }
                        get("/guilds") {
                            val session = call.sessionOrUnauthorized() ?: return@get
                            call.respond(httpClient.getGuilds(session.accessToken).emolga())
                        }
                        get("/logout") {
                            call.sessions.clear<UserSession>()
                            call.respondRedirect("https://emolga.tectoast.de/")
                        }
                    }
                }
            }
        }).start()
    }
}

private suspend fun HttpClient.getUserData(accessToken: String): DiscordUser {
    return getWithToken("https://discord.com/api/users/@me", accessToken)
}

private suspend fun HttpClient.getGuilds(accessToken: String): List<DiscordGuildData> {
    return getWithToken("https://discord.com/api/users/@me/guilds", accessToken)
}

private suspend inline fun <reified T> HttpClient.getWithToken(url: String, accessToken: String): T {
    return get(url) {
        header("Authorization", "Bearer $accessToken")
    }.body()
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

private fun List<DiscordGuildData>.emolga(): List<GuildData> {
    val emolgaguilds = EmolgaMain.emolgajda.guilds.map { it.id }
    return asSequence().filter {
        it.permissions and Permission.MANAGE_SERVER.rawValue > 0
    }.map { gd ->
        GuildData(
            gd.name,
            gd.icon?.let { "https://cdn.discordapp.com/icons/${gd.id}/$it.png" }
                ?: "assets/images/defaultservericon.png",
            gd.id,
            gd.id in emolgaguilds)
    }.sortedBy { !it.joined }.toList()
}

@Serializable
data class GuildData(val name: String, val url: String, val id: String, val joined: Boolean)

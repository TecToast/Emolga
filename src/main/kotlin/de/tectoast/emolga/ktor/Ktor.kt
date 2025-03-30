package de.tectoast.emolga.ktor

import de.tectoast.emolga.credentials.Credentials
import de.tectoast.emolga.utils.webJSON
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.User
import org.slf4j.event.Level

object Ktor {
    var devMode = false
    val injectedRouteHandlers: MutableMap<String, suspend ApplicationCall.() -> Unit> = mutableMapOf()
    var oauth2Secret: String? = null
    var server: ApplicationEngine? = null

    fun start(withYT: Boolean = true) {
        embeddedServer(CIO, port = 58700) {
            module()
        }.also { server = it }.start()
        if (withYT) {
            embeddedServer(
                CIO,
                port = Credentials.tokens.subscriber.callback.let {
                    if (":" in it) it.substringAfter("//").substringAfter(":").substringBefore("/").toInt() else 443
                }) {
                routing {
                    ytSubscriptions()
                }
            }.start()
        }
    }

    private fun Application.module() {
        installPlugins()
        routing {
            route("/api/emolga") {
                emolgaAPI()
            }
        }
    }

    private fun Application.installPlugins() {
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
    @SerialName("global_name")
    val displayName: String,
    val avatar: String?
) {
    fun emolga(): DiscordUser = this.copy(avatar = avatar?.let {
        "https://cdn.discordapp.com/avatars/${id}/${
            it + if (it.startsWith("a_")) ".gif" else ".png"
        }"
    } ?: String.format(User.DEFAULT_AVATAR_URL, ((id shr 22) % 6).toString())
    )
}

@Serializable
data class DiscordGuildData(val id: String, val name: String, val icon: String?, val permissions: Long)


@Serializable
data class GuildData(val name: String, val url: String, val id: String, val joined: Boolean)

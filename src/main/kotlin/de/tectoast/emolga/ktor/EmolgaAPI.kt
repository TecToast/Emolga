package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.ktor.routes.leagueCreate
import de.tectoast.emolga.ktor.routes.leagueManage
import de.tectoast.emolga.utils.SizeLimitedMap
import de.tectoast.emolga.utils.httpClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission

private val logger = KotlinLogging.logger {}
private suspend fun PipelineContext<*, ApplicationCall>.respondSuccess() =
    call.respond(HttpStatusCode.OK, "{\"message\": \"Hello there! Schnüffelst du hier etwa rum?\"}")
fun Route.emolgaAPI() {
    val disabled = !Ktor.devMode
    authenticate("auth-oauth-discord") {
        get("/login") {}
        get("/discordauth") {
            if (disabled) return@get respondSuccess()
            val principal: OAuthAccessTokenResponse.OAuth2 = call.principal() ?: run {
                call.respond(HttpStatusCode.BadRequest)
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
            call.respondRedirect(if (Ktor.devMode) "http://localhost:3000/" else "https://emolga.tectoast.de/")
        }
    }
    get("/userdata") {
        if (disabled) return@get respondSuccess()
        val session = call.sessions.get<UserSession>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        call.respond(httpClient.getUserData(session.accessToken).emolga())
    }
    get("/logout") {
        if (disabled) return@get respondSuccess()
        call.sessions.clear<UserSession>()
        call.respondRedirect("https://emolga.tectoast.de/")
    }
    route("/") {
        install(apiGuard)
        route("/manage") {
            leagueManage()
        }
        route("/create") {
            leagueCreate()
        }
    }

}

val apiGuard = createRouteScopedPlugin("AuthGuard") {
    onCall { call ->
        if (call.sessions.get<UserSession>() == null) {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

/**
 * Should only be used in routes that are guarded by [apiGuard]
 */
fun PipelineContext<*, ApplicationCall>.userId(): Long {
    return call.sessions.get<UserSession>()!!.userId
}

private val userdataCache = SizeLimitedMap<String, DiscordUser>()
suspend fun HttpClient.getUserData(accessToken: String): DiscordUser {
    return userdataCache.getOrPut(accessToken) {
        logger.info("Fetching user data for $accessToken")
        getWithToken("https://discord.com/api/users/@me", accessToken)
    }
}

suspend fun HttpClient.getGuilds(accessToken: String): List<DiscordGuildData> {
    return getWithToken("https://discord.com/api/users/@me/guilds", accessToken)
}

suspend inline fun <reified T> HttpClient.getWithToken(url: String, accessToken: String): T {
    return get(url) {
        header("Authorization", "Bearer $accessToken")
    }.body()
}

fun List<DiscordGuildData>.emolga(): List<GuildData> {
    val emolgaguilds = jda.guilds.map { it.id }
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

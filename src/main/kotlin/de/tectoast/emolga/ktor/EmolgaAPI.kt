package de.tectoast.emolga.ktor

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.httpClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import net.dv8tion.jda.api.Permission

fun Route.emolgaAPI() {
    authenticate("auth-oauth-discord") {
        get("/login") {}
        get("/discordauth") {
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

suspend fun HttpClient.getUserData(accessToken: String): DiscordUser {
    return getWithToken("https://discord.com/api/users/@me", accessToken)
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

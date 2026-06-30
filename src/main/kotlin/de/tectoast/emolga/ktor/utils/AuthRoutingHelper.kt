package de.tectoast.emolga.ktor.utils

import de.tectoast.emolga.domain.league.admin.repository.GuildManagerRepository
import de.tectoast.emolga.ktor.controllers.emolga.authenticated.DiscordUserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.core.annotation.Single

@Single
class AuthRoutingHelper(private val guildManagerRepo: GuildManagerRepository) {
    val userAuthGuard = createRouteScopedPlugin("UserAuthGuard") {
        onCall { call ->
            getUserId(call) ?: return@onCall call.respond(HttpStatusCode.Unauthorized)
        }
    }

    fun getUserId(call: ApplicationCall): Long? {
        return getSessionData(call)?.userId
    }

    fun getSessionData(call: ApplicationCall): DiscordUserSession? {
        return call.sessions.get<DiscordUserSession>()
    }

    suspend fun getGuildAuthContext(call: ApplicationCall): GuildAuthContext? {
        val guildId = call.parameters["guild"]?.toLongOrNull() ?: return null
        val userId = getUserId(call) ?: return null
        if (!guildManagerRepo.isAuthorized(userId, guildId)) {
            return null
        }
        return GuildAuthContext(userId, guildId)
    }

    context(routingCtx: RoutingContext)
    suspend inline fun withGuildAuth(handler: suspend RoutingContext.(GuildAuthContext) -> Unit) {
        val authContext =
            getGuildAuthContext(routingCtx.call) ?: return routingCtx.call.respond(HttpStatusCode.Unauthorized)
        routingCtx.handler(authContext)
    }

    context(routingCtx: RoutingContext)
    suspend inline fun withUserAuth(handler: suspend RoutingContext.(Long) -> Unit) {
        val userId = getUserId(routingCtx.call) ?: return routingCtx.call.respond(HttpStatusCode.Unauthorized)
        routingCtx.handler(userId)
    }

    suspend inline fun withUserAuth(call: ApplicationCall, handler: suspend ApplicationCall.(Long) -> Unit) {
        val userId = getUserId(call) ?: return call.respond(HttpStatusCode.Unauthorized)
        call.handler(userId)
    }
}


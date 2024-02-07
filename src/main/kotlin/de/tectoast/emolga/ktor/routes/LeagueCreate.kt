package de.tectoast.emolga.ktor.routes

import de.tectoast.emolga.ktor.emolga
import de.tectoast.emolga.ktor.getGuilds
import de.tectoast.emolga.ktor.sessionOrUnauthorized
import de.tectoast.emolga.utils.httpClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.leagueCreate() {
    get("/guilds") {
        val session = call.sessionOrUnauthorized() ?: return@get
        call.respond(httpClient.getGuilds(session.accessToken).emolga())
    }
}

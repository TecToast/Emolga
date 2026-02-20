package de.tectoast.emolga.ktor

import de.tectoast.emolga.database.exposed.SixVsPokeworldAuthDB
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sixVsPokeworld() {
    get("/authorized") {
        val userId =
            call.request.queryParameters["user"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
        if (SixVsPokeworldAuthDB.isAuthorized(userId)) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}
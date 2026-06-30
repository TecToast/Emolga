package de.tectoast.emolga.ktor.controllers.emolga

import de.tectoast.emolga.domain.league.liveteam.service.LiveTeamService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.bad
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [WebController::class])
class LiveTeamController(private val service: LiveTeamService) : EmolgaWebController() {
    override fun Route.setup() {
        get("/liveteam") {
            val token = Uuid.parseHexDashOrNull(call.requireQueryParameter("token")) ?: return@get call.bad()
            val result = service.getLiveTeamGraphic(
                token,
                call.requireQueryParameter("num").toIntOrNull() ?: return@get call.bad()
            )
            if (result != null) {
                call.respondBytes(result, contentType = ContentType.Image.PNG)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

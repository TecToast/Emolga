package de.tectoast.emolga.ktor.controllers.emolga

import de.tectoast.emolga.domain.web.service.MeService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class MeController(private val auth: AuthRoutingHelper, private val meService: MeService) : EmolgaWebController() {
    override fun Route.setup() {
        get("/me") {
            call.respond(auth.getSessionData(call)?.let { meService.getMeData(it.userId, it.displayName, it.avatar) }
                ?: "")
        }
    }
}
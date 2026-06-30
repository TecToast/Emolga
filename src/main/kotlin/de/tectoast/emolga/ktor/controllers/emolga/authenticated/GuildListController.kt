package de.tectoast.emolga.ktor.controllers.emolga.authenticated

import de.tectoast.emolga.domain.league.admin.service.GuildListService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class GuildListController(private val auth: AuthRoutingHelper, private val service: GuildListService) :
    EmolgaWebController() {
    override fun Route.setup() {
        get("/guilds") {
            auth.withUserAuth { userId ->
                call.respond(service.getGuildsForUser(userId))
            }
        }
    }
}

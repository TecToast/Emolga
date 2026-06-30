package de.tectoast.emolga.ktor.controllers.emolga.authenticated.guild

import de.tectoast.emolga.domain.league.member.service.LeagueParticipantDataService
import de.tectoast.emolga.ktor.EmolgaGuildWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class LeagueParticipantsController(
    private val auth: AuthRoutingHelper,
    private val dataService: LeagueParticipantDataService
) :
    EmolgaGuildWebController() {
    override fun Route.setup() {
        route("/league/{leaguename}/users") {
            get {
                auth.withGuildAuth { ctx ->
                    val leagueName = call.requirePathParameter("leaguename")
                    call.respond(
                        dataService.getLeagueParticipantData(ctx.guildId, leagueName) ?: HttpStatusCode.NotFound
                    )
                }
            }
        }
    }
}

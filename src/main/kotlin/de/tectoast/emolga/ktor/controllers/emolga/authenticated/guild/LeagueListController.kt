package de.tectoast.emolga.ktor.controllers.emolga.authenticated.guild

import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.ktor.EmolgaGuildWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class LeagueListController(private val auth: AuthRoutingHelper, private val leagueCoreRepo: LeagueCoreRepository) :
    EmolgaGuildWebController() {
    override fun Route.setup() {
        get("/leagues") {
            auth.withGuildAuth { ctx ->
                call.respond(leagueCoreRepo.getLeagueNamesByGuild(ctx.guildId))
            }
        }
    }
}
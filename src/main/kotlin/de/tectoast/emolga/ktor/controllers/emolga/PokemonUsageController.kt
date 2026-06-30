package de.tectoast.emolga.ktor.controllers.emolga

import de.tectoast.emolga.domain.league.gamedata.service.PokemonUsageService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class PokemonUsageController(private val service: PokemonUsageService) : EmolgaWebController() {
    override fun Route.setup() {
        get("/usage/{league}") {
            val leagueName = call.requirePathParameter("league")
            call.respond(
                service.getUsageDataTotal(leagueName, call.queryParameters["week"]?.toIntOrNull())
                    ?: HttpStatusCode.NotFound
            )
        }
    }
}

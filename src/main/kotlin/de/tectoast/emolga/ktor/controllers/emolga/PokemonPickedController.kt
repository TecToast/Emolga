package de.tectoast.emolga.ktor.controllers.emolga

import de.tectoast.emolga.domain.league.draft.service.util.PokemonPickedService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.bad
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class PokemonPickedController(private val service: PokemonPickedService) : EmolgaWebController() {
    override fun Route.setup() {
        get("/picked/{guild}") {
            val guild = call.requirePathParameter("guild").toLongOrNull() ?: return@get call.bad()
            call.respond(service.getPokemonPickedData(guild))
        }
    }
}

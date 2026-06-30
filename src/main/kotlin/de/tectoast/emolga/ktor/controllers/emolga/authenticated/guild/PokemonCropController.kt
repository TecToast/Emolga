package de.tectoast.emolga.ktor.controllers.emolga.authenticated.guild

import de.tectoast.emolga.domain.league.teamgraphic.model.PokemonCropData
import de.tectoast.emolga.domain.league.teamgraphic.repository.PokemonCropRepository
import de.tectoast.emolga.domain.league.teamgraphic.service.PokemonCropService
import de.tectoast.emolga.ktor.EmolgaGuildWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class PokemonCropController(
    private val auth: AuthRoutingHelper,
    private val cropRepo: PokemonCropRepository,
    private val cropService: PokemonCropService
) :
    EmolgaGuildWebController() {
    override fun Route.setup() {
        route("/teamgraphics") {
            get("/new") {
                auth.withGuildAuth { ctx ->
                    cropService.getNewPokemonToCrop(ctx.guildId)?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound)
                }
            }
            post("/data") {
                auth.withGuildAuth { ctx ->
                    val data = call.receive<PokemonCropData>()
                    cropRepo.insertPokemonCropData(ctx.guildId, data, ctx.userId)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
        }
    }
}

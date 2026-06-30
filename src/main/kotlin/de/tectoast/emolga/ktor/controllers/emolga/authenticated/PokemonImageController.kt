package de.tectoast.emolga.ktor.controllers.emolga.authenticated

import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single
import java.io.File

@Single(binds = [WebController::class])
class PokemonImageController(private val authRoutingHelper: AuthRoutingHelper) : EmolgaWebController() {
    override fun Route.setup() {
        route("/monimg") {
            install(authRoutingHelper.userAuthGuard)
            staticFiles("/monimg", File("/teamgraphics/sprites"), index = null)
        }
    }
}

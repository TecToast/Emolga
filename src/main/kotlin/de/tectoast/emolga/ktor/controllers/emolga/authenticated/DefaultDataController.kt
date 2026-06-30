package de.tectoast.emolga.ktor.controllers.emolga.authenticated

import de.tectoast.emolga.domain.util.service.DefaultDataService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import de.tectoast.emolga.ktor.utils.bad
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class DefaultDataController(private val auth: AuthRoutingHelper, private val service: DefaultDataService) :
    EmolgaWebController() {

    override fun Route.setup() {
        get("/defaultdata") {
            auth.withUserAuth {
                val path = call.request.queryParameters["path"]?.replace("?", "") ?: return@get call.bad()
                val defaultData = service.getDefaultData(path) ?: return@get call.bad()
                call.respondText(defaultData, ContentType.Application.Json)
            }
        }
    }
}


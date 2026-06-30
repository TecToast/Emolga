package de.tectoast.emolga.ktor.controllers.emolga.authenticated

import de.tectoast.emolga.domain.guildspecific.flegmon.dsb.service.DSBHostService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class DSBController(
    private val auth: AuthRoutingHelper,
    private val dsbHostService: DSBHostService,
) : EmolgaWebController() {
    override fun Route.setup() {
        route("/dsb") {
            get("/data") {
                auth.withUserAuth { userId ->
                    val dsbData =
                        dsbHostService.getDSBData(userId) ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(dsbData)
                }
            }
            sse("/sse") {
                auth.withUserAuth(call) { userId ->
                    heartbeat()
                    dsbHostService.collectSubmissions(userId) { submission -> send(submission) }
                        ?: call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}

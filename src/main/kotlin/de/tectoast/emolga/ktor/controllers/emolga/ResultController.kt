package de.tectoast.emolga.ktor.controllers.emolga

import de.tectoast.emolga.domain.league.result.service.ResultProcessService
import de.tectoast.emolga.domain.league.result.service.ResultSetupService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class ResultController(private val setupService: ResultSetupService, private val processService: ResultProcessService) :
    EmolgaWebController() {
    override fun Route.setup() {
        route("/result/{resultid}") {
            get {
                call.respond(
                    setupService.getResultDataForUser(call.parameters["resultid"]!!) ?: HttpStatusCode.NotFound
                )
            }
            post {
                call.respond(
                    processService.processResult(call.requirePathParameter("resultid"), call.receive())
                        ?.let { HttpStatusCode.Accepted } ?: HttpStatusCode.NotFound
                )
            }
        }
    }
}

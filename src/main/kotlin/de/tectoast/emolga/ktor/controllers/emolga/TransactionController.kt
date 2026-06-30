package de.tectoast.emolga.ktor.controllers.emolga

import de.tectoast.emolga.domain.league.transaction.service.TransactionService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.bad
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [WebController::class])
class TransactionController(private val service: TransactionService) : EmolgaWebController() {
    override fun Route.setup() {
        route("/transaction/{transactionid}") {
            get {
                val transactionId =
                    Uuid.parseHexDashOrNull(call.requirePathParameter("transactionid")) ?: return@get call.bad()
                call.respond(service.getTransactionData(transactionId) ?: HttpStatusCode.NotFound)
            }
            post {
                val transactionId =
                    Uuid.parseHexDashOrNull(call.requirePathParameter("transactionid")) ?: return@post call.bad()
                call.respond(service.submitTransaction(transactionId, call.receive())?.let { HttpStatusCode.Accepted }
                    ?: HttpStatusCode.BadRequest)
            }
        }
    }

}
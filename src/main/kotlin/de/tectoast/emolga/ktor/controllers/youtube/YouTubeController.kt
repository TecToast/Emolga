package de.tectoast.emolga.ktor.controllers.youtube

import de.tectoast.emolga.domain.ytgeneric.service.YouTubePushProcessingService
import de.tectoast.emolga.domain.ytgeneric.service.YouTubeSubscriptionService
import de.tectoast.emolga.ktor.WebController
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class YouTubeController(
    private val subscriptionService: YouTubeSubscriptionService,
    private val pushNotificationService: YouTubePushProcessingService
) : WebController() {
    override fun Route.setup() {
        route("/youtube") {
            get {
                val params = call.request.queryParameters
                val result = subscriptionService.handleChallengeVerification(
                    mode = params["hub.mode"],
                    topic = params["hub.topic"],
                    challenge = params["hub.challenge"]
                )
                if (result != null) {
                    call.respondText(result)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            post {
                call.respond(HttpStatusCode.Accepted)
                val signature = call.request.headers["X-Hub-Signature"] ?: return@post
                pushNotificationService.handleIncoming(call.receiveText(), signature)
            }
        }
    }
}

package de.tectoast.emolga.ktor.controllers.emolga.authenticated.guild

import de.tectoast.emolga.domain.league.signup.model.LeagueSignupConfig
import de.tectoast.emolga.domain.league.signup.model.data.ParticipantDataSet
import de.tectoast.emolga.domain.league.signup.service.SignupParticipantService
import de.tectoast.emolga.domain.league.signup.service.SignupService
import de.tectoast.emolga.ktor.EmolgaGuildWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import de.tectoast.emolga.ktor.utils.ConfiguratorRoutingService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class SignupController(
    private val auth: AuthRoutingHelper,
    private val participantService: SignupParticipantService,
    private val signupService: SignupService,
    private val configurator: ConfiguratorRoutingService
) : EmolgaGuildWebController() {
    override fun Route.setup() {
        route("/signup") {
            route("/participants") {
                participants()
            }
            route("/config") {
                config()
            }
        }
    }

    private fun Route.participants() {
        get {
            auth.withGuildAuth { ctx ->
                val result = participantService.getParticipantsForSignup(
                    ctx.guildId, call.request.queryParameters["identifier"].orEmpty()
                )
                call.respond(result ?: HttpStatusCode.NotFound)
            }
        }
        post {
            auth.withGuildAuth { ctx ->
                val data = call.receive<ParticipantDataSet>()
                val result = participantService.setConferences(
                    ctx.guildId, call.request.queryParameters["identifier"].orEmpty(), data
                )
                call.respond(result?.let { HttpStatusCode.Accepted } ?: HttpStatusCode.NotFound)
            }
        }
    }

    private fun Route.config() {
        configurator.configOption(dataHandler = { config ->
            signupService.createSignup(guildId, config)
        }, dataProvider = {
            LeagueSignupConfig(
                signupChannel = 0,
                announceChannel = 0,
                signupMessage = "Hier könnt ihr euch anmelden :)",
                maxUsers = 0
            )
        }, contextBuilder = { auth.getGuildAuthContext(it.call) })
    }
}

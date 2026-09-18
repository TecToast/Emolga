package de.tectoast.emolga.ktor.controllers.emolga

import de.tectoast.emolga.domain.web.service.LoginService
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.core.annotation.Single

@Single(binds = [WebController::class])
class CodeLoginController(private val service: LoginService) : EmolgaWebController("/codelogin") {
    override fun Route.setup() {
        get {
            val code = call.requireQueryParameter("code")
            val userSession = service.loginFromWeb(code) ?: return@get call.respondRedirect("/")
            call.sessions.set(userSession)
            call.respondRedirect("/")
        }
    }
}
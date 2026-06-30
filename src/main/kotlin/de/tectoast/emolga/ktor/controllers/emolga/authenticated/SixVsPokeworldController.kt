package de.tectoast.emolga.ktor.controllers.emolga.authenticated

import de.tectoast.emolga.domain.guildspecific.sixvspokeworld.model.SixVsPokeworldConfig
import de.tectoast.emolga.domain.guildspecific.sixvspokeworld.repository.SixVsPokeworldAuthRepository
import de.tectoast.emolga.domain.guildspecific.sixvspokeworld.repository.SixVsPokeworldConfigRepository
import de.tectoast.emolga.ktor.EmolgaWebController
import de.tectoast.emolga.ktor.WebController
import de.tectoast.emolga.ktor.utils.AuthRoutingHelper
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.koin.core.annotation.Single
import java.io.File
import java.security.MessageDigest

@Single(binds = [WebController::class])
class SixVsPokeworldController(
    private val auth: AuthRoutingHelper,
    private val authRepo: SixVsPokeworldAuthRepository,
    private val configRepo: SixVsPokeworldConfigRepository
) : EmolgaWebController() {
    private val authGuard = createRouteScopedPlugin("SixVsPokeworldAuthGuard") {
        onCall { call ->
            val userId = auth.getUserId(call) ?: return@onCall call.respond(HttpStatusCode.Unauthorized)
            if (!authRepo.isAuthorized(userId)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@onCall
            }
        }
    }

    override fun Route.setup() {
        route("/sixvspokeworld") {
            install(authGuard)
            get("/authorized") {
                call.respond(HttpStatusCode.OK)
            }
            route("/data") {
                get {
                    call.respond(configRepo.getConfig())
                }
                post {
                    val config = call.receive<SixVsPokeworldConfig>()
                    configRepo.setConfig(config)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
            route("/image") {
                post {
                    val multipart = call.receiveMultipart()
                    val fileData = multipart.readPart() as? PartData.FileItem ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing file"
                    )
                    fileData.originalFileName?.substringAfterLast('.').takeIf { it.equals("png", ignoreCase = true) }
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Only PNG files are allowed"
                        )
                    val byteArray = fileData.provider().toByteArray()
                    fileData.release()
                    val key =
                        MessageDigest.getInstance("SHA-256").digest(byteArray)
                            .joinToString("") { "%02x".format(it) } + ".png"
                    File("/sixvspokeworld/$key").writeBytes(byteArray)
                    call.respond(HttpStatusCode.OK, mapOf("key" to key))
                }
                get("{key}") {
                    val key = call.requirePathParameter("key")
                    val file = File("/sixvspokeworld/$key")
                    if (!file.exists()) {
                        return@get call.respond(HttpStatusCode.NotFound, "File not found")
                    }
                    call.respondFile(file)
                }
            }
        }
    }
}

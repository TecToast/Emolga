package de.tectoast.emolga.ktor

import de.tectoast.emolga.database.exposed.SixVsPokeworldAuthDB
import de.tectoast.emolga.utils.json.SixVsPokeworldConfig
import de.tectoast.emolga.utils.json.mdb
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.litote.kmongo.upsert
import java.io.File
import java.security.MessageDigest

val sixVsPokeworldGuard = createRouteScopedPlugin("SixVsPokeworldAuthGuard") {
    onCall { call ->
        val value = call.request.header("UserID") ?: return@onCall call.respondText(
            "No UserID provided", status = HttpStatusCode.Unauthorized
        )
        if (!SixVsPokeworldAuthDB.isAuthorized(value.toLong())) {
            call.respond(HttpStatusCode.Unauthorized)
            return@onCall
        }
    }
}

fun Route.sixVsPokeworld() {
    install(sixVsPokeworldGuard)
    get("/authorized") {
        call.respond(HttpStatusCode.OK)
    }
    route("/data") {
        get {
            call.respond(mdb.sixVsPokeworldChallenges.find().first() ?: SixVsPokeworldConfig())
        }
        post {
            val config = call.receive<SixVsPokeworldConfig>()
            mdb.sixVsPokeworldChallenges.updateOne("{}", config, options = upsert())
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
            fileData.dispose()
            val key =
                MessageDigest.getInstance("SHA-256").digest(byteArray).joinToString("") { "%02x".format(it) } + ".png"
            File("/sixvspokeworld/$key").writeBytes(byteArray)
            call.respond(HttpStatusCode.OK, mapOf("key" to key))
        }
        get("{key}") {
            val key = call.parameters["key"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing key")
            val file = File("/sixvspokeworld/$key")
            if (!file.exists()) {
                return@get call.respond(HttpStatusCode.NotFound, "File not found")
            }
            call.respondFile(file)
        }
    }

}
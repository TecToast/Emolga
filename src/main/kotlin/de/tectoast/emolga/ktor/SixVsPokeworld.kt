package de.tectoast.emolga.ktor

import de.tectoast.emolga.database.exposed.SixVsPokeworldAuthDB
import de.tectoast.emolga.utils.json.SixVsPokeworldConfig
import de.tectoast.emolga.utils.json.mdb
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.upsert

fun Route.sixVsPokeworld() {
    get("/authorized") {
        if (SixVsPokeworldAuthDB.isAuthorized(call.userId)) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
    route("/data") {
        get {
            if (!SixVsPokeworldAuthDB.isAuthorized(call.userId)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            call.respond(mdb.sixVsPokeworldChallenges.find().first() ?: SixVsPokeworldConfig())
        }
        post {
            if (!SixVsPokeworldAuthDB.isAuthorized(call.userId)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val config = call.receive<SixVsPokeworldConfig>()
            mdb.sixVsPokeworldChallenges.updateOne("{}", config, options = upsert())
            call.respond(HttpStatusCode.Accepted)
        }
    }

}
package de.tectoast.emolga.ktor

import de.tectoast.emolga.utils.webJSON
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import org.slf4j.event.Level

object Ktor {
    var devMode = false
    val injectedRouteHandlers: MutableMap<String, suspend ApplicationCall.() -> Unit> = mutableMapOf()
    var oauth2Secret: String? = null
    var artworkPath: String? = null
    var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun start(withYT: Boolean = true) {
        embeddedServer(CIO, port = 58700) {
            module()
        }.also { server = it }.start()
        if (withYT) {
            embeddedServer(
                CIO,
                port = 58701
            ) {
                routing {
                    ytSubscriptions()
                }
            }.start()
        }
    }

    private fun Application.module() {
        installPlugins()
        routing {
            route("/api/emolga") {
                emolgaAPI()
            }
        }
    }

    private fun Application.installPlugins() {
        install(ContentNegotiation) {
            json(webJSON)
        }
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)
            allowHost("emolga.tectoast.de", schemes = listOf("https"))
            allowHost("localhost:4200")
            allowCredentials = true
        }
        install(CallLogging) {
            level = Level.INFO
        }
    }
}
package de.tectoast.emolga.ktor

import de.tectoast.emolga.di.StartupTask
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.slf4j.event.Level

@Single
class KtorService(private val controllers: List<WebController>, @Named("web") private val webJson: Json) : StartupTask {

    override val priority = -5

    private val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> by lazy {
        embeddedServer(CIO, port = 58700) {
            module()
        }
    }
    private var allowAnyHost = false

    override suspend fun onStartup() {
        start()
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

    fun allowAnyHost() {
        allowAnyHost = true
    }

    private fun Application.module() {
        installPlugins()
        for (controller in controllers) {
            with(controller) {
                setupApplication()
            }
        }
        routing {
            for (controller in controllers) {
                with(controller) {
                    route(controller.basePath) {
                        setup()
                    }
                }
            }
        }
    }

    private fun Application.installPlugins() {
        install(ContentNegotiation) {
            json(webJson)
        }
        if (allowAnyHost) {
            install(CORS) {
                allowHost("*")
                allowCredentials = true
            }
        }
        install(CallLogging) {
            level = Level.INFO
        }
        install(CachingHeaders) {}
        install(SSE)
    }
}

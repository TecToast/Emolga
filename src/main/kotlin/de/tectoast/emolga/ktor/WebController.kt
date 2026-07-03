package de.tectoast.emolga.ktor

import io.ktor.server.application.*
import io.ktor.server.routing.*

abstract class WebController(val basePath: String = "/api/") {
    abstract fun Route.setup()

    open fun Application.setupApplication() {}
}

abstract class EmolgaWebController(path: String = "") : WebController("/api/emolga$path")
abstract class EmolgaGuildWebController : EmolgaWebController("/{guild}")
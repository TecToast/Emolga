package de.tectoast.emolga.ktor.utils

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingCall.bad() = this.respond(HttpStatusCode.BadRequest)
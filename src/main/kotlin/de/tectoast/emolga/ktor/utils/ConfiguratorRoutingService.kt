@file:OptIn(InternalSerializationApi::class)

package de.tectoast.emolga.ktor.utils

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import mu.KotlinLogging
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class ConfiguratorRoutingService(
    val jsonStructureBuilder: JsonStructureBuilder,
    val entityValidator: EntityValidatorService,
    @Named("strictWeb") val strictWebJson: Json
) {
    val logger = KotlinLogging.logger {}

    @OptIn(InternalSerializationApi::class)
    context(route: Route)
    inline fun <reified T : Any, C : Any> configOption(
        submitString: String? = null,
        crossinline dataHandler: suspend C.(T) -> Unit,
        crossinline dataProvider: suspend () -> T,
        crossinline contextBuilder: suspend (RoutingContext) -> C?,
        descriptor: SerialDescriptor = T::class.serializer().descriptor,
    ) {
        route.get("/struct") {
            contextBuilder(this) ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(
                jsonStructureBuilder.buildFromDescriptor(descriptor, submitString)
            )
        }
        route.get("/content") {
            contextBuilder(this) ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(dataProvider())
        }
        route.post("/save") {
            val ctx = contextBuilder(this) ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val result = runCatching {
                strictWebJson.decodeFromString(
                    T::class.serializer(), call.receiveText()
                )
            }.onFailure { it.printStackTrace() }.getOrNull() ?: return@post call.respond(
                HttpStatusCode.BadRequest
            )
            if (!entityValidator.validate(result)) {
                logger.warn("Invalid Entity submitted: $result")
                return@post call.respond(HttpStatusCode.BadRequest)
            }
            ctx.dataHandler(result)
            call.respond(HttpStatusCode.Accepted)
        }

    }
}


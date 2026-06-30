package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.core.annotation.Single

@Single
class HomeAssistantRemoteServerControlHandler(private val httpClient: HttpClient) :
    RemoteServerControlHandler<RemoteServerControlConfig.HomeAssistant> {
    override val targetClass = RemoteServerControlConfig.HomeAssistant::class

    private val logger = KotlinLogging.logger {}

    override suspend fun startServer(config: RemoteServerControlConfig.HomeAssistant): Unit =
        withContext(Dispatchers.IO) {
            logger.debug(httpClient.post("http://${config.url}/api/webhook/${config.webhookIdOn}").bodyAsText())
        }

    override suspend fun powerOff(config: RemoteServerControlConfig.HomeAssistant): Unit = withContext(Dispatchers.IO) {
        logger.debug(httpClient.post("http://${config.url}/api/webhook/${config.webhookIdOff}").bodyAsText())
    }

    override suspend fun isOn(config: RemoteServerControlConfig.HomeAssistant): Boolean {
        return when (val res = httpClient.get("http://${config.url}/api/states/${config.entityId}") {
            bearerAuth(config.token)
        }.body<HAResponseData>().state) {
            "on" -> true
            "off" -> false
            else -> error("Unknown HA response $res")
        }
    }

    @Serializable
    data class HAResponseData(val state: String)
}
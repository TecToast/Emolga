package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.koin.core.annotation.Single


@Single
class HttpRemoteServerControlHandler(private val httpClient: HttpClient) :
    RemoteServerControlHandler<RemoteServerControlConfig.Http> {
    override val targetClass = RemoteServerControlConfig.Http::class

    override suspend fun startServer(config: RemoteServerControlConfig.Http) = push(config, TURN_ON_TIME)

    override suspend fun stopServer(config: RemoteServerControlConfig.Http) = push(config, TURN_OFF_TIME)

    override suspend fun powerOff(config: RemoteServerControlConfig.Http) = push(config, POWER_OFF)

    private suspend fun push(config: RemoteServerControlConfig.Http, delay: Int) {
            httpClient.post("${config.url}/push/${config.writePin}") {
                setBody("$delay")
            }
    }

    override suspend fun isOn(config: RemoteServerControlConfig.Http) = httpClient.get("${config.url}/status/${config.readPin}").bodyAsText().contains("level=0")

    companion object {
        private const val TURN_ON_TIME = 500
        private const val TURN_OFF_TIME = 500
        private const val POWER_OFF = 5000
    }
}
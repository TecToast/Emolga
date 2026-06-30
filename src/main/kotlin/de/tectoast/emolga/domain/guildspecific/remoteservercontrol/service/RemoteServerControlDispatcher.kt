package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlConfig
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class RemoteServerControlDispatcher(handlers: List<RemoteServerControlHandler<RemoteServerControlConfig>>) :
    RemoteServerControlOperations<RemoteServerControlConfig> {
    private val registry = HandlerRegistry(handlers)

    override suspend fun startServer(config: RemoteServerControlConfig) =
        registry.getHandler(config).startServer(config)

    override suspend fun isOn(config: RemoteServerControlConfig) = registry.getHandler(config).isOn(config)

    override suspend fun stopServer(config: RemoteServerControlConfig) = registry.getHandler(config).stopServer(config)

    override suspend fun powerOff(config: RemoteServerControlConfig) = registry.getHandler(config).powerOff(config)
}
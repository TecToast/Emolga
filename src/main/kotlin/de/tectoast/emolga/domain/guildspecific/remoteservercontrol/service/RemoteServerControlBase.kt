package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlConfig
import de.tectoast.emolga.utils.handler.BaseHandler

interface RemoteServerControlOperations<C : RemoteServerControlConfig> {
    suspend fun startServer(config: C)
    suspend fun isOn(config: C): Boolean
    suspend fun stopServer(config: C)
    suspend fun powerOff(config: C)
}

interface RemoteServerControlHandler<C : RemoteServerControlConfig> : BaseHandler<C>, RemoteServerControlOperations<C> {
    override suspend fun startServer(config: C) {}
    override suspend fun isOn(config: C): Boolean = false
    override suspend fun stopServer(config: C) {}
    override suspend fun powerOff(config: C) {}
}
package de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlAction.*
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlActionData
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.repository.RemoteServerControlDelegateRepository
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.repository.RemoteServerControlRepository
import org.koin.core.annotation.Single

@Single
class RemoteServerControlService(
    private val repository: RemoteServerControlRepository,
    private val delegateRepository: RemoteServerControlDelegateRepository,
    private val dispatcher: RemoteServerControlDispatcher
) {
    suspend fun handle(data: RemoteServerControlActionData): String {
        val config = repository.getByName(data.pc) ?: return "Ungültiger PC! (${data.pc})"
        val on = dispatcher.isOn(config)
        return when (data.action) {
            START -> {
                if (on) return "Der Server ist bereits an!"
                dispatcher.startServer(config)
                "Der Server wurde gestartet!"
            }

            STATUS -> "Der Server ist ${if (on) "an" else "aus"}!"

            STOP -> {
                if (!on) return "Der Server ist bereits aus!"
                dispatcher.stopServer(config)
                "Der Server wurde gestoppt!"
            }

            POWEROFF -> {
                if (!on) return "Der Server ist bereits aus!"
                dispatcher.powerOff(config)
                "Der Server wurde poweroffed!"
            }
        }
    }

    suspend fun handleDelegate(id: Int): String {
        val data = delegateRepository.getData(id) ?: return "Dieser Button ist nicht mehr gültig!"
        return handle(data)
    }
}
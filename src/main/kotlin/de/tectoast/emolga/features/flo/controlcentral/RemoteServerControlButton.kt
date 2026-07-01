package de.tectoast.emolga.features.flo.controlcentral

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.repository.RemoteServerControlRepository
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service.RemoteServerControlDispatcher
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RemoteServerControlButton(
    private val repository: RemoteServerControlRepository,
    private val dispatcher: RemoteServerControlDispatcher
) :
    ButtonFeature<RemoteServerControlButton.Args>(::Args, ButtonSpec("remoteservercontrol")) {
    class Args : Arguments() {
        var pc by string()
        var action by enumBasic<Action>()
    }

    enum class Action {
        START, STOP, STATUS, POWEROFF;
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val config = repository.getByName(e.pc) ?: return iData.replyRaw("Ungültiger PC! (${e.pc})")
        val on = dispatcher.isOn(config)
        iData.ephemeralDefault()
        when (e.action) {
            START -> {
                if (on) return iData.replyRaw("Der Server ist bereits an!")
                dispatcher.startServer(config)
                iData.replyRaw("Der Server wurde gestartet!")
            }

            STATUS -> iData.replyRaw("Der Server ist ${if (on) "an" else "aus"}!")

            STOP -> {
                if (!on) return iData.replyRaw("Der Server ist bereits aus!")
                dispatcher.stopServer(config)
                iData.replyRaw("Der Server wurde gestoppt!")
            }

            POWEROFF -> {
                if (!on) return iData.replyRaw("Der Server ist bereits aus!")
                dispatcher.powerOff(config)
                iData.replyRaw("Der Server wurde poweroffed!")
            }

        }
    }
}
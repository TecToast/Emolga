package de.tectoast.emolga.features.flo.controlcentral

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlAction
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.model.RemoteServerControlActionData
import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service.RemoteServerControlService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RemoteServerControlButton(
    private val service: RemoteServerControlService
) :
    ButtonFeature<RemoteServerControlButton.Args>(::Args, ButtonSpec("remoteservercontrol")) {
    class Args : Arguments() {
        var pc by string("pc")
        var action by enumBasic<RemoteServerControlAction>("action")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.ephemeralDefault()
        iData.replyRaw(service.handle(RemoteServerControlActionData(e.pc, e.action)))
    }
}
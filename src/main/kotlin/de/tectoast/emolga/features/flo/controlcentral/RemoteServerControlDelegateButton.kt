package de.tectoast.emolga.features.flo.controlcentral

import de.tectoast.emolga.domain.guildspecific.remoteservercontrol.service.RemoteServerControlService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RemoteServerControlDelegateButton(
    private val service: RemoteServerControlService
) :
    ButtonFeature<RemoteServerControlDelegateButton.Args>(::Args, ButtonSpec("remoteservercontroldelegate")) {
    class Args : Arguments() {
        var id by int("id")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.ephemeralDefault()
        iData.replyRaw(service.handleDelegate(e.id))
    }
}
package de.tectoast.emolga.features.flo.resend

import de.tectoast.emolga.domain.maintenance.resend.service.ResendService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.MessageContextArgs
import de.tectoast.emolga.features.system.MessageContextSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.MessageContextFeature
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ResendMessageContext(private val service: ResendService, private val modal: ResendModal) : MessageContextFeature(
    MessageContextSpec("Resend")
) {
    context(iData: InteractionData)
    override suspend fun exec(e: MessageContextArgs) {
        service.setMessage(iData.user, e.message)
        iData.replyModal(modal())
    }
}
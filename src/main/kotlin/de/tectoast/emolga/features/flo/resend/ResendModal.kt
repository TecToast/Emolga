package de.tectoast.emolga.features.flo.resend

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.maintenance.resend.service.ResendService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ModalSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.ModalFeature
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ResendModal(private val service: ResendService, private val channelInterface: ChannelInterface) :
    ModalFeature<ResendModal.Args>(::Args, ModalSpec("resend")) {
    override val title = "Resend".k18n

    class Args : Arguments() {
        var tc by long("Channel", "Der Channel".k18n)
    }


    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        channelInterface.sendMessage(
            e.tc,
            MessageCreateBuilder().applyMessage(service.getMessage(iData.user)!!).build()
        )
        iData.done(true)
    }
}
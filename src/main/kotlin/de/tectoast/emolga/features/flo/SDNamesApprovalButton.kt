package de.tectoast.emolga.features.flo

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.league.showdownnames.repository.SDNamesRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.toShowdownUserId
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SDNamesApprovalButton(
    private val sdNamesRepo: SDNamesRepository,
    private val channelInterface: ChannelInterface
) :
    ButtonFeature<SDNamesApprovalButton.Args>(::Args, ButtonSpec("sdnamesapproval")) {
    class Args : Arguments() {
        var accept by boolean()
        var id by long()
        var username by string()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        if (e.accept) {
            sdNamesRepo.setOwnerOfName(e.username.toShowdownUserId(), e.id)
            iData.replyRaw("Der Name `${e.username}` wurde erfolgreich für <@${e.id}> registriert!", ephemeral = true)
        } else {
            iData.replyRaw("Der Name wurde nicht registriert!", ephemeral = true)
            val mid = iData.data.messageId ?: return
            channelInterface.deleteMessage(iData.tc, mid)
        }
    }
}

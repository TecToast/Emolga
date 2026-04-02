package de.tectoast.emolga.features.flo

import de.tectoast.emolga.database.exposed.SDNamesRepository
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.dependency
import org.koin.core.annotation.Single

@Single
object SDNamesApprovalButton : ButtonFeature<SDNamesApprovalButton.Args>(::Args, ButtonSpec("sdnamesapproval")) {
    class Args : Arguments() {
        var accept by boolean()
        var id by long()
        var username by string()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        if (e.accept) {
            dependency<SDNamesRepository>().setOwnerOfName(e.username, e.id)
            iData.reply("Der Name `${e.username}` wurde erfolgreich für <@${e.id}> registriert!", ephemeral = true)
        } else {
            iData.reply("Der Name wurde nicht registriert!", ephemeral = true)
            iData.textChannel.deleteMessageById(iData.message.idLong).queue()
        }
    }
}

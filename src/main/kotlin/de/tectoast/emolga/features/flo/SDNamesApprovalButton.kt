package de.tectoast.emolga.features.flo

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.ButtonFeature
import de.tectoast.emolga.features.ButtonSpec

object SDNamesApprovalButton : ButtonFeature<SDNamesApprovalButton.Args>(::Args, ButtonSpec("sdnamesapproval")) {
    class Args : Arguments() {
        var accept by boolean()
        var id by long()
        var username by string()
    }

    context(InteractionData)
    override suspend fun exec(e: Args) = buttonEvent {
        if (e.accept) {
            SDNamesDB.replace(e.username, e.id)
            reply("Der Name `${e.username}` wurde erfolgreich für <@${e.id}> registriert!", ephemeral = true)
        } else {
            reply("Der Name wurde nicht registriert!", ephemeral = true)
            hook.deleteMessageById(idLong).queue()
        }
    }
}

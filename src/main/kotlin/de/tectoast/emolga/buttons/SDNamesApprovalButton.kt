package de.tectoast.emolga.buttons

import de.tectoast.emolga.database.exposed.SDNamesDB
import dev.minn.jda.ktx.messages.reply_
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class SDNamesApprovalButton : ButtonListener("sdnamesapproval") {
    val logger = KotlinLogging.logger {}
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val args = name.split(";")
        val accept = args[0].toBoolean()
        if (accept) {
            val id = args[1].toLong()
            val username = args[2]
            SDNamesDB.replace(username, id)
            e.reply_("Der Name `$username` wurde erfolgreich für <@${id}> registriert!", ephemeral = true).queue()
        } else {
            e.reply_("Der Name wurde nicht registriert!", ephemeral = true).queue()
        }
    }
}

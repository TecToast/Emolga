package de.tectoast.emolga.modals

import de.tectoast.emolga.utils.sql.managers.SDNamesManager
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class AddSDNameModal : ModalListener("addsdname") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val sdname = e.getValue("name")!!.asString
        val id = e.getValue("id")?.asString?.toLongOrNull() ?: e.user.idLong
        if (SDNamesManager.addIfAbsent(sdname, id)) {
            e.reply("Dein Showdown-Name `$sdname` wurde hinzugefügt!").setEphemeral(true).queue()
        } else {
            e.reply("Der Showdown-Name `$sdname` ist bereits im System!").setEphemeral(true).queue()
        }
    }
}

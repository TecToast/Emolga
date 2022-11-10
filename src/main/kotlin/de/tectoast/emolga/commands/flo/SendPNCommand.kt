package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class SendPNCommand : Command("sendpn", "Sendet PNs", CommandCategory.Flo) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("user", "User", "Der User, der die PN erhalten soll", ArgumentManagerTemplate.DiscordType.ID)
            add("text", "Text", "Der Text der PN", ArgumentManagerTemplate.Text.any())
        }
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.jda.openPrivateChannelById(e.arguments.getID("user")).queue { channel ->
            channel.sendMessage(e.arguments.getText("text")).queue()
        }
        e.reply("Done!", ephermal = true)
    }
}

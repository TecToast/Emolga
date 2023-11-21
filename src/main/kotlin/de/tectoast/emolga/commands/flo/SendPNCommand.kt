package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.*

object SendPNCommand : TestableCommand<SendPNCommandArgs>("sendpn", "Sendet PNs", CommandCategory.Flo) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("user", "User", "Der User, der die PN erhalten soll", ArgumentManagerTemplate.DiscordType.ID)
            add("text", "Text", "Der Text der PN", ArgumentManagerTemplate.Text.any())
        }
        slash()
    }

    override fun fromGuildCommandEvent(e: GuildCommandEvent) = SendPNCommandArgs(
        e.arguments.getID("user"),
        e.arguments.getText("text")
    )

    context (CommandData)
    override suspend fun exec(e: SendPNCommandArgs) {
        jda.openPrivateChannelById(e.user).flatMap { channel ->
            channel.sendMessage(e.text)
        }.queue()
        reply("Done!", ephemeral = true)
    }
}

class SendPNCommandArgs(val user: Long, val text: String) : CommandArgs

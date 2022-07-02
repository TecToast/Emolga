package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel

class DeleteuntilCommand :
    Command("deleteuntil", "Löscht alle Nachrichten bis zur angegebenen ID", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "tc",
                "Text-Channel",
                "Der Channel, in dem gelöscht werden soll, sonst der, in dem der Command geschrieben wurde",
                ArgumentManagerTemplate.DiscordType.CHANNEL,
                true
            )
            .add(
                "mid",
                "Message-ID",
                "Die Message-ID, bis zu der gelöscht werden soll",
                ArgumentManagerTemplate.DiscordType.ID
            )
            .setExample("!deleteuntil #Banane 839470836624130098")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val m = e.message!!
        val tco = e.textChannel
        val tc: TextChannel
        val channels = m.mentions.getChannels(TextChannel::class.java)
        tc = if (channels.size > 0) channels[0] else tco
        val mid = if (e.hasArg(1)) e.getArg(1) else e.getArg(0)
        try {
            tc.retrieveMessageById(mid).complete()
        } catch (ex: Exception) {
            tco.sendMessage("In diesem Channel gibt es keine Nachricht mit dieser ID!").queue()
            return
        }
        val todel = ArrayList<Message>()
        for (message in tc.iterableHistory) {
            if (message.id == mid) break
            todel.add(message)
        }
        tc.deleteMessages(todel).queue()
        tco.sendMessage("Success!").queue()
    }
}
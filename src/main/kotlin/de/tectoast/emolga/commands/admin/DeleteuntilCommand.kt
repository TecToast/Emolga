package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.entities.Message

class DeleteuntilCommand :
    Command("deleteuntil", "Löscht alle Nachrichten bis zur angegebenen ID", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "tc",
                "Text-Channel",
                "Der Channel, dessen Nachrichten gelöscht werden sollen, sonst der, in dem der Command geschrieben wurde",
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
        val args = e.arguments
        val tc = args.getOrDefault("tc", e.textChannel)
        val mid = args.getID("mid")
        try {
            tc.retrieveMessageById(mid).complete()
        } catch (ex: Exception) {
            e.reply("Diese Nachricht existiert nicht!")
            return
        }
        val todel = ArrayList<Message>()
        for (message in tc.iterableHistory) {
            if (message.idLong == mid) break
            todel.add(message)
        }
        tc.deleteMessages(todel).queue()
        e.reply("Success!")
    }
}
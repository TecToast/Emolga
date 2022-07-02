package de.tectoast.emolga.commands.admin

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class CountuntilCommand :
    Command("countuntil", "Zählt die Nachrichten bis zur angegebenen Nachricht", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "tc",
                "Text-Channel",
                "Der Channel, in dem gezählt werden soll, sonst der, in dem der Command geschrieben wurde",
                ArgumentManagerTemplate.DiscordType.CHANNEL,
                true
            )
            .add(
                "mid",
                "Message-ID",
                "Die Message-ID, bis zu der gezählt werden soll",
                ArgumentManagerTemplate.DiscordType.ID
            )
            .setExample("!countuntil #Banane 839470836624130098")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        /*Message m = e.getMessage();
        String msg = m.getContentRaw();
        TextChannel tco = e.getChannel();
        TextChannel tc;
        String mid;
        if (e.hasArg(1)) {
            tc = m.getMentionedChannels().get(0);
            mid = e.getArg(1);
        } else {
            tc = tco;
            mid = e.getArg(0);
        }*/
        val args = e.arguments!!
        val tc = args.getOrDefault("tc", e.textChannel)
        val mid = args.getID("mid")
        try {
            tc.retrieveMessageById(mid).complete()
        } catch (ex: Exception) {
            e.reply("Diese Nachricht existiert nicht!")
            return
        }
        var i = 0
        for (message in tc.iterableHistory) {
            i++
            if (message.idLong == mid) break
        }
        e.reply("Bis zu dieser ID wurden $i Nachrichten geschickt!")
    }
}
package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class CountuntilCommand extends Command {
    public CountuntilCommand() {
        super("countuntil", "Zählt die Nachrichten bis zur angegebenen Nachricht", CommandCategory.Admin);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("tc", "Text-Channel", "Der Channel, in dem gezählt werden soll, sonst der, in dem der Command geschrieben wurde", ArgumentManagerTemplate.DiscordType.CHANNEL, true)
                .add("mid", "Message-ID", "Die Message-ID, bis zu der gezählt werden soll", ArgumentManagerTemplate.DiscordType.ID)
                .setExample("!countuntil #Banane 839470836624130098")
                .build()
        );
    }

    @Override
    public void process(GuildCommandEvent e) {
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
        ArgumentManager args = e.getArguments();
        TextChannel tc = args.getOrDefault("tc", e.getChannel());
        long mid = args.getID("mid");
        try {
            tc.retrieveMessageById(mid).complete();
        } catch (Exception ex) {
            e.reply("Diese Nachricht existiert nicht!");
            return;
        }
        int i = 0;
        for (Message message : tc.getIterableHistory()) {
            i++;
            if (message.getIdLong() == mid) break;
        }
        e.reply("Bis zu dieser ID wurden " + i + " Nachrichten geschickt!");
    }
}

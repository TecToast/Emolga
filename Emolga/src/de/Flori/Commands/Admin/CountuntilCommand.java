package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class CountuntilCommand extends Command {
    public CountuntilCommand() {
        super("countuntil", "`!countuntil [Text-Channel] <Message-ID>` Zählt die Nachrichten bis zur angegebenen Nachricht", CommandCategory.Admin);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Message m = e.getMessage();
        String msg = m.getContentRaw();
        String[] split = msg.split(" ");
        TextChannel tco = e.getChannel();
        TextChannel tc;
        String mid;
        if (split.length == 3) {
            tc = m.getMentionedChannels().get(0);
            mid = split[2];
        } else {
            tc = tco;
            mid = split[1];
        }
        try {
            tc.retrieveMessageById(mid).complete();
        } catch (Exception ex) {
            tco.sendMessage("Diese Nachricht existiert nicht!").queue();
            return;
        }
        int i = 0;
        for (Message message : tc.getIterableHistory()) {
            i++;
            if (message.getId().equals(mid)) break;
        }
        tco.sendMessage("Bis zu dieser ID wurden " + i + " Nachrichten geschickt!").queue();
    }
}

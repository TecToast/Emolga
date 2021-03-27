package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class CountuntilCommand extends Command {
    public CountuntilCommand() {
        super("countuntil", "`!countuntil [Text-Channel] <Message-ID>` Zählt die Nachrichten bis zur angegebenen Nachricht", CommandCategory.Admin);
    }

    @Override
    public void process(GuildCommandEvent e) {
        Message m = e.getMessage();
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

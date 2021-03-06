package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;

public class DeleteuntilCommand extends Command {
    public DeleteuntilCommand() {
        super("deleteuntil", "`!deleteuntil [Text-Channel] <Message-ID>` Löscht alle Nachrichten bis zur angegebenen ID", CommandCategory.Admin);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Message m = e.getMessage();
        String msg = e.getMessage().getContentRaw();
        System.out.println("msg = " + msg);
        TextChannel tco = e.getChannel();
        String[] split = msg.split(" ");
        TextChannel tc;
        if (m.getMentionedChannels().size() > 0) tc = m.getMentionedChannels().get(0);
        else tc = tco;
        String mid = split.length == 3 ? split[2] : split[1];
        try {
            tc.retrieveMessageById(mid).complete();
        } catch (Exception ex) {
            tco.sendMessage("In diesem Channel gibt es keine Nachricht mit dieser ID!").queue();
            return;
        }
        ArrayList<Message> todel = new ArrayList<>();
        for (Message message : tc.getIterableHistory()) {
            if (message.getId().equals(mid)) break;
            todel.add(message);
        }
        tc.deleteMessages(todel).queue();
        tco.sendMessage("Success!").queue();
    }
}

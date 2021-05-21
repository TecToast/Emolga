package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;

public class DeleteuntilCommand extends Command {
    public DeleteuntilCommand() {
        super("deleteuntil", "Löscht alle Nachrichten bis zur angegebenen ID", CommandCategory.Admin);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("tc", "Text-Channel", "Der Channel, in dem gelöscht werden soll, sonst der, in dem der Command geschrieben wurde", ArgumentManagerTemplate.DiscordType.CHANNEL, true)
                .add("mid", "Message-ID", "Die Message-ID, bis zu der gelöscht werden soll", ArgumentManagerTemplate.DiscordType.ID)
                .setExample("!deleteuntil #Banane 839470836624130098")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Message m = e.getMessage();
        String msg = e.getMessage().getContentRaw();
        System.out.println("msg = " + msg);
        TextChannel tco = e.getChannel();
        TextChannel tc;
        if (m.getMentionedChannels().size() > 0) tc = m.getMentionedChannels().get(0);
        else tc = tco;
        String mid = e.hasArg(1) ? e.getArg(1) : e.getArg(0);
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

package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class DeleteuntilCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        String msg = e.getMessage().getId();
        TextChannel tco = e.getChannel();
        String mid = msg.substring(13);
        try {
            tco.retrieveMessageById(mid).complete();
        } catch (Exception ex) {
            tco.sendMessage("In diesem Channel gibt es keine Nachricht mit dieser ID!").queue();
            return;
        }
        for (Message message : tco.getIterableHistory()) {
            if (message.getId().equals(mid)) break;
            message.delete().queue();
        }
        tco.sendMessage("Success!").queue();
    }

    public DeleteuntilCommand() {
        super("deleteuntil", "`!deleteuntil <Message-ID>` Löscht alle Nachrichten bis zur angegebenen ID", CommandCategory.Admin);
    }
}

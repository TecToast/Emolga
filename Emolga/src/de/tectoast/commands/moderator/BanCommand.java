package de.tectoast.commands.moderator;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class BanCommand extends Command {
    public BanCommand() {
        super("ban", "`!ban <User> <Grund>` Bannt den User", CommandCategory.Moderator);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        String raw = m.getContentRaw();
        if (m.getMentionedMembers().size() != 1) {
            //tco.sendMessage("Du musst einen Spieler taggen!").queue();
            return;
        }
        Member mem = m.getMentionedMembers().get(0);
        String reason;
        try {
            reason = raw.substring(raw.indexOf(">") + 2);
        } catch (Exception ignored) {
            reason = "Nicht angegeben";
        }
        ban(tco, mem, reason);
    }
}

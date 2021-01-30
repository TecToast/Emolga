package de.tectoast.commands.moderator;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class KickCommand extends Command {
    public KickCommand() {
        super("kick", "`!kick <User> <Grund>` Kickt den User", CommandCategory.Moderator);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Message m = e.getMessage();
        String raw = m.getContentRaw();
        TextChannel tco = e.getChannel();
        Guild g = e.getGuild();
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
        kick(tco, mem, reason);
    }
}

package de.tectoast.commands.moderator;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class UnmuteCommand extends Command {
    public UnmuteCommand() {
        super("unmute", "`!unmute <User>` Entmutet den User", CommandCategory.Moderator);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        JSONObject json = getEmolgaJSON();
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        Guild g = e.getGuild();
        if (m.getMentionedMembers().size() != 1) {
            return;
        }
        Member mem = m.getMentionedMembers().get(0);
        unmute(tco, mem);
    }
}

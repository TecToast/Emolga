package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

public class UnmuteCommand extends Command {
    public UnmuteCommand() {
        super("unmute", "`!unmute <User>` Entmutet den User", CommandCategory.Moderator);
    }

    @Override
    public void process(CommandEvent e) {
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

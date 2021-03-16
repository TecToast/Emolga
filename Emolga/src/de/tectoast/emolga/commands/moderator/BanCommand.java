package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class BanCommand extends Command {
    public BanCommand() {
        super("ban", "`!ban <User> <Grund>` Bannt den User", CommandCategory.Moderator);
    }

    @Override
    public void process(CommandEvent e) {
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

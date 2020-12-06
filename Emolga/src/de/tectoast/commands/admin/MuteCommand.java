package de.tectoast.commands.admin;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class MuteCommand extends Command {
    public MuteCommand() {
        super("mute", "`!mute <User> <Grund>` Mutet den User wegen des angegebenen Grundes", CommandCategory.Admin, "712035338846994502");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Message m = e.getMessage();
        String raw = m.getContentRaw();
        TextChannel tco = e.getChannel();
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
        mute(tco, e.getMember(), mem, reason);
    }
}

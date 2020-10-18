package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;

public class BanCommand extends Command {
    public BanCommand() {
        super("warn", "`!ban <User> <Grund>` Bannt den User", CommandCategory.Admin, "712035338846994502");
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
        String reason = "Nicht angegeben";
        try {
            reason = raw.substring(raw.indexOf(">") + 2);
        } catch (Exception ignored) {

        }
        ban(tco, mem, reason);
    }
}

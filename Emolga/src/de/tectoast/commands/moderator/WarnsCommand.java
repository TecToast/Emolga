package de.tectoast.commands.moderator;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;

public class WarnsCommand extends Command {


    public WarnsCommand() {
        super("warns", "`!warns <User>` Zeigt alle Verwarnungen des Users an", CommandCategory.Moderator);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        JSONObject json = getEmolgaJSON().getJSONObject("warns");
        String gid = e.getGuild().getId();
        if (m.getMentionedMembers().size() != 1) {
            tco.sendMessage("Du musst einen User taggen!").queue();
            return;
        }
        Member mem = m.getMentionedMembers().get(0);
        if (!json.has(gid)) json.put(gid, new JSONArray());
        JSONArray arr = json.getJSONArray(gid);
        if (arr.length() == 0) {
            tco.sendMessage("Es wurde bisher niemand auf diesem Server verwarnt!").queue();
            return;
        }
        StringBuilder str = new StringBuilder();
        for (Object o : arr) {
            JSONObject obj = (JSONObject) o;
            if (obj.getString("user").equals(mem.getId())) {
                str.append("Von: <@").append(obj.getString("mod")).append(">\nGrund: ").append(obj.getString("reason")).append("\n\n");
            }
        }
        if (str.toString().equals("")) {
            tco.sendMessage("Dieser User hat bisher keine Verwarnungen!").queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.CYAN);
            builder.setTitle("Verwarnungen von " + mem.getEffectiveName());
            builder.setDescription(str.toString());
            tco.sendMessage(builder.build()).queue();
        }
    }
}

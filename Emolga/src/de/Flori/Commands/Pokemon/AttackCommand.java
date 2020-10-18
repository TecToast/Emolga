package de.Flori.Commands.Pokemon;

import de.Flori.Commands.Command;

import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class AttackCommand extends Command {

    public AttackCommand() {
        super("attack", "`!attack <Name>` Zeigt, welche Mons eine Attacke erlernen können", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        JSONObject json = getMovesJSON();
        ArrayList<String> mons = new ArrayList<>();
        String str = getGerName(msg.substring(8));
        if (!str.split(";")[0].equals("atk")) {
            tco.sendMessage("Das ist keine Attacke!").queue();
            return;
        }
        String atk = str.split(";")[1];
        for (String s : json.keySet()) {
            if (json.getJSONObject(s).getJSONObject("learnset").keySet().contains(atk)) {
                mons.add(json.getJSONObject(s).getString("name"));
            }
        }
        Collections.sort(mons);
        StringBuilder s = new StringBuilder();
        for (String mon : mons) {
            s.append(mon).append("\n");
            if (s.length() > 1900) {
                tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(atk + " können:").setDescription(s).build()).queue();
                s = new StringBuilder();
            }
        }
        tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(atk + " können:").setDescription(s).build()).queue();
    }
}

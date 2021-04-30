package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class AttackCommand extends Command {

    public AttackCommand() {
        super("attack", "`!attack <Name>` Zeigt, welche Mons eine Attacke erlernen können", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        JSONObject json = getLearnsetJSON(getModByGuild(e));
        ArrayList<String> mons = new ArrayList<>();
        Translation t = getEnglNameWithType(msg.substring(8), getModByGuild(e));
        if (!t.isFromType(Translation.Type.MOVE)) {
            tco.sendMessage("Das ist keine Attacke!").queue();
            return;
        }
        String atk = t.getTranslation();
        for (String s : json.keySet()) {
            if(!json.getJSONObject(s).has("learnset")) continue;
            if (json.getJSONObject(s).getJSONObject("learnset").keySet().contains(toSDName(atk))) {
                String name = s;
                if(s.endsWith("alola")) name = getGerNameNoCheck(s.substring(0,s.length() - 5)) + "-Alola";
                else if(s.endsWith("galar")) name = getGerNameNoCheck(s.substring(0,s.length() - 5)) + "-Galar";
                else if(s.endsWith("unova")) name = getGerNameNoCheck(s.substring(0,s.length() - 5)) + "-Unova";
                else {
                    Translation gerName = getGerName(s);
                    if(gerName.isSuccess()) name = gerName.getTranslation();
                    else {
                        for (int i = 1; i <= s.length(); i++) {
                            String sub = s.substring(0, i);
                            gerName = getGerName(sub);
                            if(gerName.isSuccess()) {
                                name = gerName.getTranslation();
                                break;
                            }
                        }
                    }
                }
                String[] split = name.split("-");
                if(split.length > 1) mons.add(getGerNameNoCheck(split[0]) + "-" + split[1]);
                else mons.add(getGerNameNoCheck(name));
            }
        }
        Collections.sort(mons);
        StringBuilder s = new StringBuilder();
        for (String mon : mons) {
            s.append(mon).append("\n");
            if (s.length() > 1900) {
                tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(getGerNameNoCheck(atk) + " können:").setDescription(s).build()).queue();
                s = new StringBuilder();
            }
        }
        tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle(getGerNameNoCheck(atk) + " können:").setDescription(s).build()).queue();
    }
}

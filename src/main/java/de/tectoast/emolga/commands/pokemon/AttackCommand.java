package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jsolf.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class AttackCommand extends Command {

    public AttackCommand() {
        super("attack", "Zeigt, welche Mons eine Attacke erlernen können", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .addEngl("move", "Attacke", "Die Attacke, die angeschaut werden soll", Translation.Type.MOVE)
                .setExample("!attack Tarnsteine")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject ljson = getLearnsetJSON(getModByGuild(e));
        JSONObject djson = getDataJSON(getModByGuild(e));
        ArrayList<String> mons = new ArrayList<>();
        String atk = e.getArguments().getTranslation("move").getTranslation();
        for (String s : djson.keySet()) {
            if (s.endsWith("totem")) continue;
            JSONObject data = djson.getJSONObject(s);
            if (data.getInt("num") <= 0) continue;
            if (!ljson.has(s)) continue;
            if (!ljson.getJSONObject(s).has("learnset")) continue;
            if (ljson.getJSONObject(s).getJSONObject("learnset").keySet().contains(toSDName(atk))) {
                String name;
                /*if (s.endsWith("alola")) name = getGerNameNoCheck(s.substring(0, s.length() - 5)) + "-Alola";
                else if (s.endsWith("galar")) name = getGerNameNoCheck(s.substring(0, s.length() - 5)) + "-Galar";
                else if (s.endsWith("unova")) name = getGerNameNoCheck(s.substring(0, s.length() - 5)) + "-Unova";
                else {*/
                if(s.equals("nidoranf")) name = "Nidoran-F";
                else if(s.equals("nidoranm")) name = "Nidoran-M";
                else {
                    Translation gerName = getGerName(s);
                    if (gerName.isSuccess()) name = gerName.getTranslation();
                    else {
                        name = getGerNameNoCheck(data.getString("baseSpecies")) + "-" + data.getString("forme");
                    }
                }
                //}
                /*String[] split = name.split("-");
                System.out.println("name = " + name);
                if (split.length > 1) mons.add(getGerNameNoCheck(split[0]) + "-" + split[1]);
                else mons.add(getGerNameNoCheck(name));*/
                mons.add(name);
            }
        }
        mons.removeIf(Objects::isNull);
        Collections.sort(mons);
        StringBuilder s = new StringBuilder();
        for (String mon : mons) {
            s.append(mon).append("\n");
            if (s.length() > 1900) {
                e.reply(new EmbedBuilder().setColor(Color.CYAN).setTitle(getGerNameNoCheck(atk) + " können:").setDescription(s).build());
                s = new StringBuilder();
            }
        }
        e.reply(new EmbedBuilder().setColor(Color.CYAN).setTitle(getGerNameNoCheck(atk) + " können:").setDescription(s).build());
    }
}

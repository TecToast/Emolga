package de.tectoast.commands.pokemon;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CombinationCommand extends Command {
    public CombinationCommand() {
        super("combination", "`!combination [Attacke|Fähigkeit],[Attacke|Fähigkeit] ...` Zeigt, welche pokemon die angegeben Attacken lernen bzw. die Fähigkeiten haben können", CommandCategory.Pokemon);
    }

    public static boolean containsNotAll(JSONObject mon, ArrayList<String> list) {
        ArrayList<String> l = new ArrayList<>(list);
        for (String s : mon.keySet()) {
            l.remove(mon.getString(s));
        }
        return l.size() != 0;
    }

    public static boolean containsNotAll(JSONArray arr, ArrayList<String> list) {
        List<String> l = arr.toList().stream().map(o -> (String) o).collect(Collectors.toList());
        for (String s : list) {
            if (!l.contains(s)) return true;
        }
        return false;
    }

    public static boolean containsNotAll(Set<String> set, ArrayList<String> list) {
        for (String s : list) {
            if (!set.contains(s)) return true;
        }
        return false;
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        if (msg.length() <= 13) tco.sendMessage(getHelp()).queue();
        String args = msg.substring(13);
        ArrayList<String> atks = new ArrayList<>();
        ArrayList<String> abis = new ArrayList<>();
        ArrayList<String> types = new ArrayList<>();
        ArrayList<String> egg = new ArrayList<>();
        for (String s : args.split(",")) {
            String str = getGerName(s.trim());
            if (str.equals("") || str.startsWith("pkmn")) {
                tco.sendMessage("**" + s + "** ist kein valides Argument!").queue();
                return;
            }
            if (str.startsWith("atk")) atks.add(getEnglName(str.split(";")[1]));
            else if (str.startsWith("abi")) abis.add(getEnglName(str.split(";")[1]));
            else if (str.startsWith("type")) types.add(getEnglName(str.split(";")[1]));
            else if (str.startsWith("egg")) egg.add(getEnglName(str.split(";")[1]));
        }
        JSONObject data = getDataJSON();
        JSONObject moves = getMovesJSON();
        ArrayList<String> mons = new ArrayList<>();
        for (String s : getMonList()) {
            JSONObject mon = data.getJSONObject(s);
            if (abis.size() > 0 && containsNotAll(mon.getJSONObject("abilities"), abis)) continue;
            if (types.size() > 0 && containsNotAll(mon.getJSONArray("types"), types)) continue;
            if (egg.size() > 0 && containsNotAll(mon.getJSONArray("eggGroups"), egg)) continue;
            String string;
            if (mon.has("baseSpecies")) string = mon.getString("baseSpecies").toLowerCase();
            else string = s.toLowerCase();
            if (atks.size() > 0 && containsNotAll(moves.getJSONObject(string.replace("-", "")).getJSONObject("learnset").keySet(), atks))
                continue;
            mons.add(mon.getString("name"));
            /*
            JSONObject mon = data.getJSONObject(s);
            if (!containsAll(mon.getString("abilities"), abis)) continue;
            if (!containsAll(mon.getString("types"), types)) continue;
            if (!containsAll(mon.getString("egggroup"), egg)) continue;
            JSONObject forms = mon.getJSONObject("moves");
            for (String st : forms.keySet()) {
                if (containsAll(forms.getString(st), atks)) mons.add(s + (st.equals("Normal") ? "" : " (" + st + ")"));
            }*/
        }
        if (mons.size() == 0) {
            tco.sendMessage("Kein pokemon hat diese Kombination an Attacken/Fähigkeiten/Typen!").queue();
        } else {
            Collections.sort(mons);
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Diese Kombination haben:").setColor(new Color(0, 255, 255));
            StringBuilder str = new StringBuilder();
            for (String o : mons) {
                str.append(o).append("\n");
                if (str.length() > 1900) {
                    tco.sendMessage(builder.setDescription(str.toString()).build()).queue();
                    builder = new EmbedBuilder();
                    builder.setTitle("Diese Kombination haben:").setColor(new Color(0, 255, 255));
                    str = new StringBuilder();
                }
            }
            builder.setDescription(str.toString());
            //tco.sendMessage(builder.build()).queue();
            Collections.sort(mons);
            tco.sendMessage(new EmbedBuilder().setColor(Color.CYAN).setTitle("Diese Kombination haben:").setDescription(String.join("\n", mons)).build()).queue();
        }
    }
}

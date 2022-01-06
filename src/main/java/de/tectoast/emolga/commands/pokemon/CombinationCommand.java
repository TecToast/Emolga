package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class CombinationCommand extends Command {
    public CombinationCommand() {
        super("combination", "Zeigt, welche Pokemon die angegeben Attacken lernen bzw. die Fähigkeiten haben können", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!combination <Typ|Attacke|Eigruppe|Fähigkeit>, <Typ|Attacke|Eigruppe|Fähigkeit> usw.",
                "!combination Water, Donnerblitz"));
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
    public void process(GuildCommandEvent e) {
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
        String modByGuild = getModByGuild(e);
        for (String s : args.split(",")) {
            Translation t = getGerName(s.trim(), modByGuild, false);
            if (t.isEmpty() || t.isFromType(Translation.Type.POKEMON)) {
                tco.sendMessage("**" + s + "** ist kein valides Argument!").queue();
                return;
            }
            String trans = t.getTranslation();
            if (t.isFromType(Translation.Type.MOVE)) atks.add(getSDName(trans, modByGuild));
            else if (t.isFromType(Translation.Type.ABILITY)) abis.add(getEnglName(trans, modByGuild));
            else if (t.isFromType(Translation.Type.TYPE)) types.add(getEnglName(trans, modByGuild));
            else if (t.isFromType(Translation.Type.EGGGROUP)) egg.add(getEnglName(trans, modByGuild));
        }
        JSONObject data = getDataJSON(modByGuild);
        JSONObject moves = getLearnsetJSON(modByGuild);
        ArrayList<String> mons = new ArrayList<>();
        for (String s : getMonList(modByGuild)) {
            JSONObject mon = data.getJSONObject(s);
            if (abis.size() > 0 && containsNotAll(mon.getJSONObject("abilities"), abis)) continue;
            if (types.size() > 0 && containsNotAll(mon.getJSONArray("types"), types)) continue;
            if (egg.size() > 0 && containsNotAll(mon.getJSONArray("eggGroups"), egg)) continue;
            //System.out.println("s = " + s);
            String string = null;
            boolean isRegion = false;
            for (String form : Arrays.asList("alola", "galar", "unova")) {
                if (mon.optString("forme", "").toLowerCase().contains(form)) isRegion = true;
            }
            if (!isRegion) {
                if (mon.has("baseSpecies")) string = mon.getString("baseSpecies");
            }
            if (string == null) string = s;

            if (atks.size() > 0) {
                if (!moves.has(toSDName(string))) continue;
                if (!moves.getJSONObject(toSDName(string)).has("learnset")) continue;
                if (containsNotAll(moves.getJSONObject(toSDName(string)).getJSONObject("learnset").keySet(), atks))
                    continue;
            }
            if (mon.getInt("num") < 0) continue;
            String name = data.getJSONObject(s).getString("name");
            String[] split = name.split("-");
            if (split.length > 1) mons.add(getGerNameNoCheck(split[0]) + "-" + split[1]);
            else mons.add(getGerNameNoCheck(name));
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
            tco.sendMessage("Kein Pokemon hat diese Kombination an Attacken/Fähigkeiten/Typen!").queue();
        } else {
            Collections.sort(mons);
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Diese Kombination haben:").setColor(new Color(0, 255, 255));
            StringBuilder str = new StringBuilder();
            for (String o : mons) {
                str.append(o).append("\n");
                if (str.length() > 1900) {
                    tco.sendMessageEmbeds(builder.setDescription(str.toString()).build()).queue();
                    builder = new EmbedBuilder();
                    builder.setTitle("Diese Kombination haben:").setColor(new Color(0, 255, 255));
                    str = new StringBuilder();
                }
            }
            builder.setDescription(str.toString());
            //tco.sendMessage(builder.build()).queue();
            Collections.sort(mons);
            tco.sendMessageEmbeds(new EmbedBuilder().setColor(Color.CYAN).setTitle("Diese Kombination haben:").setDescription(String.join("\n", mons)).build()).queue();
        }
    }
}

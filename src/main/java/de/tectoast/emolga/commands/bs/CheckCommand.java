package de.tectoast.emolga.commands.bs;

import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Google;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class CheckCommand extends Command {
    public CheckCommand() {
        super("check", "`!check <pokemon> [Ball]` Schaut, ob jemand dieses pokemon in diesem Ball oder überhaupt besitzt", CommandCategory.BS);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        String[] split = msg.split(" ");
        String pokemon = split[1];
        String ball;
        if (split.length == 2) {
            ball = "NONE";
        } else {
            String pokeball = split[2];
            if (pokeball.equalsIgnoreCase("Pokeball")) pokeball = "Pokéball";
            Optional<String> opt = balls.stream().filter(pokeball::equalsIgnoreCase).findFirst();
            if (opt.isEmpty()) {
                tco.sendMessage("Dieser Ball existiert nicht!").queue();
                return;
            }
            ball = opt.get();
        }
        String mon;
        Optional<String> optmon = mons.stream().filter(pokemon::equalsIgnoreCase).findFirst();
        if (optmon.isPresent()) {
            mon = optmon.get();
        } else {
            Translation t;
            if (pokemon.toLowerCase().startsWith("a-")) t = getGerName(pokemon.substring(2)).before("A-");
            else if (pokemon.toLowerCase().startsWith("g-")) t = getGerName(pokemon.substring(2)).before("A-");
            else t = getGerName(pokemon);
            if (!t.isFromType(Translation.Type.POKEMON)) {
                tco.sendMessage("Dieses Pokemon existiert nicht!").queue();
                return;
            }
            mon = t.getTranslation();
        }
        if ((mon.equals("Hopplo") || mon.equals("Memmeon") || mon.equals("Chimpep")) && !ball.equals("Pokéball") && !ball.equals("NONE")) {
            tco.sendMessage("Die Starter können nur in einem normalen Pokéball sein!").queue();
            return;
        }
        if (!mons.contains(mon)) {
            tco.sendMessage("Dieses Pokemon steht nicht im Tauschdokument!").queue();
            return;
        }
        JSONObject json = getEmolgaJSON();
        if (!json.has("tradedoc")) {
            tco.sendMessage("Es hat sich bisher keiner registriert!").queue();
            return;
        }
        JSONObject names = json.getJSONObject("tradedoc");
        if (!ball.equals("NONE")) {
            String range = "VFs und Ballmons!" + (ball.equals("Ultraball") ? "AA" : (char) (balls.indexOf(ball) + 68)) + (mons.indexOf(mon) + 3);
            List<List<Object>> list = Google.get(tradesid, range, false, false);
            ArrayList<Integer> l = list == null ? new ArrayList<>() : Arrays.stream(((String) list.get(0).get(0)).split(";")).map(Integer::parseInt).sorted().collect(Collectors.toCollection(ArrayList::new));

            if (l.isEmpty()) {
                tco.sendMessage("Niemand hat ein " + mon + " in einem " + ball + "!").queue();
            } else {
                ArrayList<String> owns = l.stream().map(i -> EmolgaMain.emolgajda.getGuildById("712035338846994502").retrieveMemberById(names.getString(String.valueOf(i))).complete().getEffectiveName()).collect(Collectors.toCollection(ArrayList::new));
                if (owns.size() == 1) {
                    tco.sendMessage("**" + owns.get(0) + "** hat ein " + mon + " in einem " + ball + "!").queue();
                } else {
                    tco.sendMessage("Folgende Leute haben ein " + mon + " in einem " + ball + ": **" + String.join("**, **", owns) + "**").queue();
                }
            }
        } else {
            String range = "VFs und Ballmons!D" + (mons.indexOf(mon) + 3) + ":AA" + (mons.indexOf(mon) + 3);
            List<List<Object>> get = Google.get(tradesid, range, false, false);
            if (get == null) {
                tco.sendMessage("Niemand hat ein " + mon + "!").queue();
                return;
            }
            List<Object> list = get.get(0);
            HashMap<String, String> map = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                String s = (String) list.get(i);
                if (s.equals("")) continue;
                ArrayList<Integer> l = Arrays.stream(s.split(";")).map(Integer::parseInt).sorted().collect(Collectors.toCollection(ArrayList::new));
                ArrayList<String> owns = l.stream().map(j -> EmolgaMain.emolgajda.getGuildById("712035338846994502").retrieveMemberById(names.getString(String.valueOf(j))).complete().getEffectiveName()).collect(Collectors.toCollection(ArrayList::new));
                map.put(balls.get(i), "**" + String.join("**, **", owns) + "**");
            }
            StringBuilder builder = new StringBuilder(mon + ":\n");
            for (String s : balls) {
                if (map.containsKey(s)) {
                    builder.append(s).append(": ").append(map.get(s)).append("\n");
                }
            }
            tco.sendMessage(builder.toString()).queue();
        }

    }
}

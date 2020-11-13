package de.Flori.Commands.Pokemon;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class SpeedCommand extends Command {

    public SpeedCommand() {
        super("speed", "`!speed <Pokemon1 Pokemon2 ...>` Zeigt die Init-Base und die maximale Initiative der Pokemon auf Level 100 an.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            String s = msg.substring(7);
            String[] mons;
            if (!s.contains("\n"))
                mons = s.split(" ");
            else mons = s.split("\n");
            StringBuilder str = new StringBuilder();
            for (String mon : mons) {
                System.out.println("mon = " + mon);
                int bs;
                String ger;
                if (mon.startsWith("M-")) {
                    String st = getGerName(mon.substring(2));
                    if (st.equals("") || !st.startsWith("pkmn")) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = st.split(";")[1];
                    bs = getWikiJSON().getJSONObject("pkmndata").getJSONObject(ger).getJSONObject("stats").getJSONObject("Mega-" + ger).getInt("init");
                } else if (mon.startsWith("A-")) {
                    String st = getGerName(mon.substring(2));
                    if (st.equals("") || !st.startsWith("pkmn")) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = st.split(";")[1];
                    JSONObject stats = getWikiJSON().getJSONObject("pkmndata").getJSONObject(ger).getJSONObject("stats");
                    if (stats.has("Alola-" + ger))
                        bs = stats.getJSONObject("Alola-" + ger).getInt("init");
                    else bs = stats.getJSONObject(ger).getInt("init");
                } else if (mon.startsWith("G-")) {
                    String string = getGerName(mon.substring(2));
                    if (string.equals("") || !string.startsWith("pkmn")) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = string.split(";")[1];
                    JSONObject stats = getWikiJSON().getJSONObject("pkmndata").getJSONObject(ger).getJSONObject("stats");
                    if (stats.has("Galar-" + ger))
                        bs = stats.getJSONObject("Galar-" + ger).getInt("init");
                    else bs = stats.getJSONObject(ger).getInt("init");
                } else {
                    if (mon.startsWith("Amigento") || mon.startsWith("Silvally")) {
                        str.append(mon).append(": 95 -> 317\n");
                        continue;
                    } else if (mon.startsWith("Rotom")) {
                        if (mon.equalsIgnoreCase("Rotom")) str.append("Rotom: 91 -> 309\n");
                        else str.append("Rotom: 86 -> 298\n");
                        continue;
                    }
                    String string = getGerName(mon);
                    if (string.equals("") || !string.startsWith("pkmn")) {
                        tco.sendMessage(mon + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = string.split(";")[1];
                    JSONObject stats = getWikiJSON().getJSONObject("pkmndata").getJSONObject(ger).getJSONObject("stats");
                    if (stats.has(ger)) {
                        str.append(ger).append(": ").append(stats.getJSONObject(ger).getInt("init")).append(" -> ").append((int) ((2 * stats.getJSONObject(ger).getInt("init") + 99) * 1.1)).append("\n");
                        continue;
                    }
                    if (stats.keySet().size() > 1) {
                        for (String st : stats.keySet()) {
                            if (st.equalsIgnoreCase(ger)) {
                                str.append(ger).append(": ").append(stats.getJSONObject(st).getInt("init")).append(" -> ").append((int) ((2 * stats.getJSONObject(st).getInt("init") + 99) * 1.1)).append("\n");
                                continue;
                            } else if (st.startsWith("Mega-")) continue;
                            str.append(ger).append(" ").append(st).append(": ").append(stats.getJSONObject(st).getInt("init")).append(" -> ").append((int) ((2 * stats.getJSONObject(st).getInt("init") + 99) * 1.1)).append("\n");
                        }
                        continue;
                    }
                    bs = stats.getJSONObject(ger).getInt("init");
                }
                int speed = (int) ((2 * bs + 99) * 1.1);
                String prefix = "";
                if (mon.startsWith("M-")) prefix = "M-";
                else if (mon.startsWith("A-")) prefix = "A-";
                else if (mon.startsWith("G-")) prefix = "G-";
                str.append(prefix).append(ger).append(": ").append(bs).append(" -> ").append(speed).append("\n");
            }
            tco.sendMessage(str.toString()).queue();
        } catch (Exception ex) {
            ex.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
        }
    }
}

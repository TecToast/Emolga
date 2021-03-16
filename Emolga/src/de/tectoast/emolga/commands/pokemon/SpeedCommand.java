package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

public class SpeedCommand extends Command {

    public SpeedCommand() {
        super("speed", "`!speed <Pokemon1 Pokemon2 ...>` Zeigt die Init-Base und die maximale Initiative der pokemon auf Level 100 an.", CommandCategory.Pokemon);
    }

    @Override
    public void process(CommandEvent e) {
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
            StringBuilder str = new StringBuilder();//
            JSONObject datajson = getDataJSON(getModByGuild(e));
            for (String mon : mons) {
                mon = mon.trim();
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
                    bs = datajson.getJSONObject(getSDName(ger) + "mega").getJSONObject("baseStats").getInt("spe");
                } else if (mon.startsWith("A-")) {
                    String st = getGerName(mon.substring(2));
                    if (st.equals("") || !st.startsWith("pkmn")) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = st.split(";")[1];
                    bs = datajson.getJSONObject(getSDName(ger) + "alola").getJSONObject("baseStats").getInt("spe");
                } else if (mon.startsWith("G-")) {
                    String string = getGerName(mon.substring(2));
                    if (string.equals("") || !string.startsWith("pkmn")) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = string.split(";")[1];
                    bs = datajson.getJSONObject(getSDName(ger) + "galar").getJSONObject("baseStats").getInt("spe");
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
                    bs = getDataJSON(getModByGuild(e)).getJSONObject(getSDName(ger)).getJSONObject("baseStats").getInt("spe");
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

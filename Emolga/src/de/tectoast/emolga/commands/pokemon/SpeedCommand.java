package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpeedCommand extends Command {

    public SpeedCommand() {
        super("speed", "`!speed <Pokemon1 Pokemon2 ...>` Zeigt die Init-Base und die maximale Initiative der pokemon auf Level 100 an.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
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
            ArrayList<SpeedMon> speedMons = new ArrayList<>();
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
                        speedMons.add(new SpeedMon("Amigento", 95, 317));
                        continue;
                    } else if (mon.startsWith("Rotom")) {
                        if (mon.equalsIgnoreCase("Rotom")) speedMons.add(new SpeedMon("Rotom", 91, 309));
                        else speedMons.add(new SpeedMon(mon, 86, 298));
                        continue;
                    }
                    Optional<String> op = sdex.keySet().stream().filter(mon::equalsIgnoreCase).findFirst();
                    if (op.isPresent()) {
                        ger = op.get();
                        String englname = getEnglName(ger.split("-")[0]);
                        bs = datajson.getJSONObject(toSDName(englname + sdex.get(mon))).getJSONObject("baseStats").getInt("spe");
                    } else {
                        String string = getGerName(mon);
                        if (string.equals("") || !string.startsWith("pkmn")) {
                            tco.sendMessage(mon + " ist kein Pokemon!").queue();
                            return;
                        }
                        ger = string.split(";")[1];
                        bs = datajson.getJSONObject(getSDName(ger)).getJSONObject("baseStats").getInt("spe");
                    }
                }
                int speed = (int) ((2 * bs + 99) * 1.1);
                String prefix = "";
                if (mon.startsWith("M-")) prefix = "M-";
                else if (mon.startsWith("A-")) prefix = "A-";
                else if (mon.startsWith("G-")) prefix = "G-";
                speedMons.add(new SpeedMon(prefix + ger, bs, speed));
            }
            speedMons.sort(null);
            tco.sendMessage(speedMons.stream().map(SpeedMon::toString).collect(Collectors.joining("\n"))).queue();
        } catch (Exception ex) {
            ex.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
        }
    }

    private static class SpeedMon implements Comparable<SpeedMon> {
        String monName;
        int baseSpeed;
        int maxSpeed;

        public SpeedMon(String monName, int baseSpeed, int maxSpeed) {
            this.monName = monName;
            this.baseSpeed = baseSpeed;
            this.maxSpeed = maxSpeed;
        }

        @Override
        public String toString() {
            return monName + ": " + baseSpeed + " -> " + maxSpeed;
        }

        @Override
        public int compareTo(@NotNull SpeedCommand.SpeedMon o) {
            return Integer.compare(o.baseSpeed, baseSpeed);
        }
    }
}

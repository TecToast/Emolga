package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpeedCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(SpeedCommand.class);

    public SpeedCommand() {
        super("speed", "Zeigt die Init-Base und die maximale Initiative der pokemon auf Level 100 an.", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!speed <Pokemon1> <Pokemon2> usw.", "!speed Galvantula M-Gallade Primarene Bisaflor"));
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
                mons = s.split("\\s+");
            else mons = s.split("\\s*\n\\s*");
            ArrayList<SpeedMon> speedMons = new ArrayList<>();
            JSONObject datajson = getDataJSON(getModByGuild(e));
            for (String mon : mons) {
                logger.info("mon = " + mon);
                int bs;
                String ger;
                if (mon.startsWith("M-")) {
                    Translation st = getGerName(mon.substring(2));
                    if (st.isEmpty() || !st.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = st.getTranslation();
                    bs = datajson.getJSONObject(getSDName(ger) + "mega").getJSONObject("baseStats").getInt("spe");
                } else if (mon.startsWith("A-")) {
                    Translation st = getGerName(mon.substring(2));
                    if (st.isEmpty() || !st.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = st.getTranslation();
                    bs = datajson.getJSONObject(getSDName(ger) + "alola").getJSONObject("baseStats").getInt("spe");
                } else if (mon.startsWith("G-")) {
                    Translation st = getGerName(mon.substring(2));
                    if (st.isEmpty() || !st.isFromType(Translation.Type.POKEMON)) {
                        tco.sendMessage(mon.substring(2) + " ist kein Pokemon!").queue();
                        return;
                    }
                    ger = st.getTranslation();
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
                        Translation t = getGerName(mon);
                        if (t.isEmpty() || !t.isFromType(Translation.Type.POKEMON)) {
                            tco.sendMessage(mon + " ist kein Pokemon!").queue();
                            return;
                        }
                        ger = t.getTranslation();
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

    private record SpeedMon(String monName, int baseSpeed, int maxSpeed) implements Comparable<SpeedMon> {

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

package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Google;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabelleCommand extends Command {
    public TabelleCommand() {
        super("tabelle", "Zeigt die Tabelle der Liga an", CommandCategory.Pokemon, 712035338846994502L, 518008523653775366L);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        disable();
        //overrideHelp.put("518008523653775366", "`!tabelle` Zeigt die Tabelle der angebenen Liga an");
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String gid = tco.getGuild().getId();
        if (gid.equals("712035338846994502")) {
            List<List<Object>> list = Google.get("1cG_7HDC9v16PCEyqJdZzdSgOZtKqW2nKV5mgE7LVuk0", "Tabelle!D3:J20", false, false);
            StringBuilder str = new StringBuilder("```");
            ArrayList<String> send = new ArrayList<>();
            ArrayList<Integer> name = new ArrayList<>();
            ArrayList<Integer> vic = new ArrayList<>();
            ArrayList<Integer> kills = new ArrayList<>();
            ArrayList<Integer> deaths = new ArrayList<>();
            ArrayList<Integer> dif = new ArrayList<>();
            for (int i = 0; i < 18; i++) {
                List<Object> l = list.get(i);
                String s = (i + 1) + ". " + (i < 9 ? " " : "") + l.get(0);
                name.add(s.length());
                vic.add(((String) l.get(2)).length());
                kills.add(((String) l.get(4)).length());
                deaths.add(((String) l.get(5)).length());
                dif.add(((String) l.get(6)).length());
            }
            int maxname = Collections.max(name);
            //logger.info("maxname = " + maxname);
            int maxvic = Collections.max(vic);
            int maxkills = Collections.max(kills);
            int maxdeaths = Collections.max(deaths);
            int maxdif = Collections.max(dif);
            String seperator = "   ";
            for (int i = 0; i < 18; i++) {
                List<Object> l = list.get(i);
                str.append(expandTo((i + 1) + ". " + (i < 9 ? " " : "") + l.get(0), maxname)).append(seperator).append(expandTo((String) l.get(2), maxvic)).append(" S.").append(seperator).append(expandTo((String) l.get(4), maxkills)).append(" K.").append(seperator).append(expandTo((String) l.get(5), maxdeaths)).append(" D.").append(seperator).append(expandTo((String) l.get(6), maxdif)).append(" Dif.\n");
                //logger.info();
            }
            //str.append(i + 1).append(". ").append(l.get(0)).append(" - ").append(l.get(2)).append(" S. - ").append(l.get(4)).append(" K. - ").append(l.get(5)).append(" D. - ").append(l.get(6)).append(" Dif.\n");
            tco.sendMessage(str.append("```").toString()).queue();
        } else if (gid.equals("518008523653775366")) {
            List<List<Object>> list = Google.get("1uVLF4_O-qop80CU6mNJucADn2_XeaJ4oCaU1z46QWtM", "Tabelle!C3:J14", false, false);
            StringBuilder str = new StringBuilder("```");
            ArrayList<Integer> name = new ArrayList<>();
            ArrayList<Integer> points = new ArrayList<>();
            ArrayList<Integer> kills = new ArrayList<>();
            ArrayList<Integer> deaths = new ArrayList<>();
            ArrayList<Integer> dif = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                List<Object> l = list.get(i);
                String s = (i + 1) + ". " + (i < 9 ? " " : "") + ((String) l.get(0)).replace("\n", " ");
                name.add(s.length());
                points.add(((String) l.get(1)).length());
                kills.add(((String) l.get(5)).length());
                deaths.add(((String) l.get(6)).length());
                dif.add(((String) l.get(7)).length());
            }
            int maxname = Collections.max(name);
            //logger.info("maxname = " + maxname);
            int maxpoints = Collections.max(points);
            int maxkills = Collections.max(kills);
            int maxdeaths = Collections.max(deaths);
            int maxdif = Collections.max(dif);
            String seperator = "   ";
            for (int i = 0; i < 12; i++) {
                List<Object> l = list.get(i);
                str.append(expandTo((i + 1) + ". " + (i < 9 ? " " : "") + ((String) l.get(0)).replace("\n", " "), maxname)).append(seperator).append(expandTo((String) l.get(1), maxpoints)).append(" P.").append(seperator).append(expandTo((String) l.get(5), maxkills)).append(" K.").append(seperator).append(expandTo((String) l.get(6), maxdeaths)).append(" D.").append(seperator).append(expandTo((String) l.get(7), maxdif)).append(" Dif.\n");
                //logger.info();
            }
            //str.append(i + 1).append(". ").append(l.get(0)).append(" - ").append(l.get(2)).append(" S. - ").append(l.get(4)).append(" K. - ").append(l.get(5)).append(" D. - ").append(l.get(6)).append(" Dif.\n");
            tco.sendMessage(str.append("```").toString()).queue();
        }
    }
}

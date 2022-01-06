package de.tectoast.emolga.commands.bs;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Google;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CheckMonsCommand extends Command {
    public CheckMonsCommand() {
        super("checkmons", "`!checkmons @User` Zeigt, welche Mons dieser User bereits ins Tauschdokument eingetragen hat", CommandCategory.BS);
    }

    @Override
    public void process(GuildCommandEvent e) {
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        Member mem;
        boolean isSelf;
        if (m.getMentionedMembers().size() > 0) {
            mem = m.getMentionedMembers().get(0);
            isSelf = false;
        } else {
            mem = e.getMember();
            isSelf = true;
        }
        JSONObject doc = getEmolgaJSON().getJSONObject("tradedoc");
        int num = -1;
        for (String s : doc.keySet()) {
            if (doc.getString(s).equals(mem.getId())) {
                num = Integer.parseInt(s);
                break;
            }
        }
        if (num == -1) {
            tco.sendMessage((isSelf ? "Du b" : "**" + mem.getEffectiveName() + "** ") + "ist nicht im Tauschdokument registriert!").queue();
            return;
        }
        List<List<Object>> get = Google.get(tradesid, "VFs und Ballmons!B3:AA249", false, false);
        HashMap<String, List<String>> map = new HashMap<>();
        for (List<Object> objects : get) {
            if (objects.size() == 1) continue;
            for (int i = 2; i < objects.size(); i++) {
                String str = (String) objects.get(i);
                if (Arrays.asList(str.split(";")).contains(String.valueOf(num))) {
                    String mon = (String) objects.get(0);
                    if (!map.containsKey(mon)) map.put(mon, new ArrayList<>());
                    map.get(mon).add(balls.get(i - 2));
                }
            }
        }
        if (map.isEmpty()) {
            tco.sendMessage((isSelf ? "Du hast" : "**" + mem.getEffectiveName() + "** hat") + " noch kein pokemon ins Tauschdokument eingetragen!").queue();
            return;
        }
        StringBuilder s = new StringBuilder("Eingetragene pokemon von " + (isSelf ? "dir" : "**" + mem.getEffectiveName() + "**") + ":\n");
        map.keySet().stream().sorted().forEach(mon -> s.append(mon).append(": ").append(String.join(", ", map.get(mon))).append("\n"));
        tco.sendMessage(s.toString()).queue();
    }
}

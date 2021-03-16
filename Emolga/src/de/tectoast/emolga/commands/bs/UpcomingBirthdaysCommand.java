package de.tectoast.emolga.commands.bs;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class UpcomingBirthdaysCommand extends Command {
    public UpcomingBirthdaysCommand() {
        super("upcomingbirthdays", "`!upcomingbirthdays` Zeigt die naheliegende Geburtstage an", CommandCategory.BS);
    }

    @Override
    public void process(CommandEvent e) {
        JSONObject json = getEmolgaJSON();
        TextChannel tco = e.getChannel();
        if (!json.has("birthdays")) {
            tco.sendMessage("Es wurde bisher kein Geburtstag eingetragen!").queue();
            return;
        }
        JSONObject birthdays = json.getJSONObject("birthdays");
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.HOUR_OF_DAY, 0);
        curr.set(Calendar.MINUTE, 0);
        curr.set(Calendar.SECOND, 0);
        HashMap<String, Calendar> map = new HashMap<>();
        for (String s : birthdays.keySet()) {
            JSONObject obj = birthdays.getJSONObject(s);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, obj.getInt("day"));
            c.set(Calendar.MONTH, obj.getInt("month") - 1);
            c.set(Calendar.YEAR, curr.getTimeInMillis() - c.getTimeInMillis() >= 0 ? c.get(Calendar.YEAR) + 1 : c.get(Calendar.YEAR));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            long dif = c.getTimeInMillis() - curr.getTimeInMillis();
            if (dif <= 864000000 && dif >= -432000000) {
                map.put(s, c);
            }
        }
        if (map.isEmpty()) {
            tco.sendMessage("Es gibt keine nahegelegenen Geburtstage!").queue();
            return;
        }
        StringBuilder str = new StringBuilder("Naheliegende Geburtstage:\n\n");
        for (String s : map.keySet().stream().sorted(Comparator.comparing(s -> map.get(s).getTimeInMillis())).collect(Collectors.toList())) {
            Calendar c = map.get(s);
            str.append("`").append(getWithZeros(c.get(Calendar.DAY_OF_MONTH), 2)).append(".").append(getWithZeros(c.get(Calendar.MONTH) + 1, 2)).append(".").append("`: ").append(e.getGuild().retrieveMemberById(s).complete().getEffectiveName()).append("\n");
        }
        tco.sendMessage(str.toString()).queue();
            /*(s1, s2) -> {
            Calendar c1 = map.get(s1);
            Calendar c2 = map.get(s2);
            return Long.compare(c1.getTimeInMillis(), c2.getTimeInMillis());
        }*/
    }
}

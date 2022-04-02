package de.tectoast.emolga.commands.flegmon;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.PepeCommand;
import de.tectoast.emolga.database.Database;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;

public class NextBirthdayCommand extends PepeCommand {
    public NextBirthdayCommand() {
        super("nextbirthday", "Zeigt die naheliegende Geburtstage an");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        aliases.add("nextbirthdays");
    }

    @Override
    public void process(GuildCommandEvent e) throws SQLException {
        JSONObject json = getEmolgaJSON();
        TextChannel tco = e.getChannel();
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.HOUR_OF_DAY, 0);
        curr.set(Calendar.MINUTE, 0);
        curr.set(Calendar.SECOND, 0);
        HashMap<Long, Calendar> map = new HashMap<>();
        ResultSet set = Database.select("SELECT * FROM birthdays");
        while (set.next()) {
            long id = set.getLong("userid");
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, set.getInt("day"));
            c.set(Calendar.MONTH, set.getInt("month") - 1);
            c.set(Calendar.YEAR, curr.getTimeInMillis() - c.getTimeInMillis() >= 0 ? c.get(Calendar.YEAR) + 1 : c.get(Calendar.YEAR));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            long dif = c.getTimeInMillis() - curr.getTimeInMillis();
            if (dif <= 1209600000/* && dif >= -432000000*/) {
                map.put(id, c);
            }
        }
        if (map.isEmpty()) {
            tco.sendMessage("Es gibt keine nahegelegenen Geburtstage!").queue();
            return;
        }
        StringBuilder str = new StringBuilder("Die nächsten Geburtstage:\n\n");
        HashMap<Long, String> names = new HashMap<>();
        e.getGuild().retrieveMembersByIds(map.keySet()).get().forEach(mem -> names.put(mem.getIdLong(), mem.getEffectiveName()));
        for (long id : map.keySet().stream().sorted(Comparator.comparing(s -> map.get(s).getTimeInMillis())).toList()) {
            Calendar c = map.get(id);
            str.append("`").append(getWithZeros(c.get(Calendar.DAY_OF_MONTH), 2)).append(".").append(getWithZeros(c.get(Calendar.MONTH) + 1, 2)).append(".").append("`: ").append(names.get(id)).append("\n");
        }
        tco.sendMessage(str.toString()).queue();
            /*(s1, s2) -> {
            Calendar c1 = map.get(s1);
            Calendar c2 = map.get(s2);
            return Long.compare(c1.getTimeInMillis(), c2.getTimeInMillis());
        }*/
    }
}

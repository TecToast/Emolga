package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class NatureCommand extends Command {

    final HashMap<String, String> statnames;

    public NatureCommand() {
        super("nature", "`!nature <Wesen>` Zeigt an, welche Werte dieses Wesen beeinflusst", CommandCategory.Pokemon);
        statnames = new HashMap<>();
        statnames.put("atk", "Atk");
        statnames.put("def", "Def");
        statnames.put("spa", "SpAtk");
        statnames.put("spd", "SpDef");
        statnames.put("spe", "Init");
    }

    @Override
    public void process(GuildCommandEvent e) throws SQLException {
        if(!e.hasArg(0)) {
            e.reply("Du musst ein Wesen angeben!");
            return;
        }
        ResultSet set = getTranslation(e.getArg(0), Command.getModByGuild(e));
        if(set.next()) {
            if(!set.getString("type").equals("nat")) {
                e.reply("Das ist kein Wesen!");
                return;
            }
            JSONObject o = getWikiJSON().getJSONObject("natures").getJSONObject(set.getString("englishname"));
            StringBuilder b = new StringBuilder(set.getString("germanname") + "/" + set.getString("englishname") + ":\n");
            if(o.has("plus")) {
                b.append(statnames.get(o.getString("plus"))).append("+\n").append(statnames.get(o.getString("minus"))).append("-");
            } else {
                b.append("Neutral");
            }
            e.reply(b.toString());
        } else {
            e.reply("Das ist kein Wesen!");
        }
    }
}

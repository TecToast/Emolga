package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn;
import net.dv8tion.jda.api.entities.Guild;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.sql.ResultSet;
import java.util.*;

public class WarnsManager extends DataManager {

    final LongColumn USERID = new LongColumn("userid", this);
    final LongColumn MODID = new LongColumn("modid", this);
    final LongColumn GUILDID = new LongColumn("guildid", this);
    final StringColumn REASON = new StringColumn("reason", this);
    final TimestampColumn TIMESTAMP = new TimestampColumn("timestamp", this);

    public WarnsManager() {
        super("warns");
        setColumns(USERID, MODID, GUILDID, REASON, TIMESTAMP);
    }

    public void warn(long userid, long modid, long guildid, String reason) {
        insert(userid, modid, guildid, reason, null);
    }

    public JSONArray getWarns(Guild g) {
        try {
            ResultSet set = GUILDID.getAll(g.getIdLong());
            JSONArray arr = new JSONArray();
            List<JSONObject> l = new LinkedList<>();
            while (set.next()) {
                l.add(new JSONObject()
                        .put("userid", USERID.getValue(set))
                        .put("modid", MODID.getValue(set))
                        .put("reason", REASON.getValue(set))
                        .put("timestamp", TIMESTAMP.getValue(set).getTime())
                );
            }
            Set<Long> idstocheck = new HashSet<>();
            l.stream().map(j -> j.getLong("userid")).forEach(idstocheck::add);
            l.stream().map(j -> j.getLong("modid")).forEach(idstocheck::add);
            HashMap<Long, String> names = new HashMap<>();
            g.retrieveMembersByIds(idstocheck).get().forEach(mem -> names.put(mem.getIdLong(), mem.getEffectiveName()));
            for (JSONObject j : l) {
                long uid = j.getLong("userid");
                String name = names.get(uid);
                if(name == null) continue;
                arr.put(new JSONObject().put("name", name).put("id", String.valueOf(uid)).put("reason", j.getString("reason")).put("mod", names.get(j.getLong("modid"))).put("timestamp", j.getLong("timestamp")));
            }
            return arr;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new JSONArray().put(new JSONObject().put("name", "ERROR"));
    }
}

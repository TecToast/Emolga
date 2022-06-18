package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.tectoast.emolga.utils.sql.base.Condition.and;

public class BanManager extends DataManager {

    private static final Logger logger = LoggerFactory.getLogger(BanManager.class);

    final LongColumn USERID = new LongColumn("userid", this);
    final StringColumn USERNAME = new StringColumn("username", this);
    final LongColumn MODID = new LongColumn("modid", this);
    final LongColumn GUILDID = new LongColumn("guildid", this);
    final StringColumn REASON = new StringColumn("reason", this);
    final TimestampColumn TIMESTAMP = new TimestampColumn("timestamp", this);
    final TimestampColumn EXPIRES = new TimestampColumn("expires", this);


    public BanManager() {
        super("bans");
        setColumns(USERID, USERNAME, MODID, GUILDID, REASON, TIMESTAMP, EXPIRES);
    }

    public void ban(long userid, String username, long modid, long guildid, String reason, @Nullable Timestamp expires) {
        insert(userid, username, modid, guildid, reason, null, expires);
    }

    public JSONArray getBans(Guild g) {
        try {
            List<JSONObject> l = read(selectAll(GUILDID.check(g.getIdLong())), s -> {
                return map(s, set -> new JSONObject()
                        .put("userid", USERID.getValue(set))
                        .put("username", USERNAME.getValue(set))
                        .put("modid", MODID.getValue(set))
                        .put("reason", REASON.getValue(set))
                        .put("timestamp", TIMESTAMP.getValue(set).getTime()));
            });
            JSONArray arr = new JSONArray();
            Set<Long> idstocheck = new HashSet<>();
            l.stream().map(j -> j.getLong("modid")).forEach(idstocheck::add);
            HashMap<Long, String> names = new HashMap<>();
            g.retrieveMembersByIds(idstocheck).get().forEach(mem -> names.put(mem.getIdLong(), mem.getEffectiveName()));
            for (JSONObject j : l) {
                long uid = j.getLong("userid");
                arr.put(new JSONObject().put("name", j.getString("username")).put("id", String.valueOf(uid)).put("reason", j.getString("reason")).put("mod", names.get(j.getLong("modid"))).put("timestamp", j.getLong("timestamp")));
            }
            return arr;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new JSONArray().put(new JSONObject().put("name", "ERROR"));
    }

    public JSONObject unbanWebsite(Guild g, long userid) {
        JSONObject o = new JSONObject();
        logger.info("userid = " + userid);
        logger.info("g.getIdLong() = " + g.getIdLong());
        g.unban(UserSnowflake.fromId(userid)).queue();
        return unban(userid, g.getIdLong()) > 0 ? o.put("success", "Entbannung erfolgreich!") : o.put("error", "Die Person war gar nicht gebannt!");
    }

    public int unban(long userid, long guildid) {
        return delete(and(USERID.check(userid), GUILDID.check(guildid)));
    }
}

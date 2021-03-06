package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn;

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
}

package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn;

import javax.annotation.Nullable;
import java.sql.Timestamp;

public class MuteManager extends DataManager {

    final LongColumn USERID = new LongColumn("userid", this);
    final LongColumn MODID = new LongColumn("modid", this);
    final LongColumn GUILDID = new LongColumn("guildid", this);
    final StringColumn REASON = new StringColumn("reason", this);
    final TimestampColumn TIMESTAMP = new TimestampColumn("timestamp", this);
    final TimestampColumn EXPIRES = new TimestampColumn("expires", this);


    public MuteManager() {
        super("mutes");
        setColumns(USERID, MODID, GUILDID, REASON, TIMESTAMP, EXPIRES);
    }

    public void mute(long userid, long modid, long guildid, String reason, @Nullable Timestamp expires) {
        insert(userid, modid, guildid, reason, null, expires);
    }
}

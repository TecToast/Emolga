package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;

public class ReplayCheckManager extends DataManager {

    final LongColumn CHANNELID = new LongColumn("channelid", this);
    final LongColumn MESSAGEID = new LongColumn("messageid", this);

    public ReplayCheckManager() {
        super("replaycheck");
        setColumns(CHANNELID, MESSAGEID);
    }

    // TODO ReplayCheckManager und die restlichen Manager fertig machen
}

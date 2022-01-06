package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;

public class NMLAnnouncementsManager extends DataManager {

    final LongColumn CHANNELID = new LongColumn("channelid", this);

    public NMLAnnouncementsManager() {
        super("nmlannouncements");
        setColumns(CHANNELID);
    }
}

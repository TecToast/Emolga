package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;

public class SpoilerTagsManager extends DataManager {

    final LongColumn GUILDID = new LongColumn("replay", this);

    public SpoilerTagsManager() {
        super("analysis");
        setColumns(GUILDID);
    }

    public boolean check(long guildid) {
        return GUILDID.retrieveValue(GUILDID, guildid) != null;
    }
}

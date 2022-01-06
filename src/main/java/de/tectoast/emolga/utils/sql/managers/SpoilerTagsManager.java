package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;

public class SpoilerTagsManager extends DataManager {

    final LongColumn GUILDID = new LongColumn("guildid", this);

    public SpoilerTagsManager() {
        super("spoilertags");
        setColumns(GUILDID);
    }

    public boolean check(long guildid) {
        return GUILDID.retrieveValue(GUILDID, guildid) != null;
    }

    public boolean delete(long guildid) {
        return delete(GUILDID.check(guildid)) != 0;
    }
}

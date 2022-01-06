package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;

public class MusicGuildsManager extends DataManager {

    LongColumn GUILDID = new LongColumn("guildid", this);

    public MusicGuildsManager() {
        super("musicguilds");
        setColumns(GUILDID);
    }

    public void addGuild(long gid) {
        insert(gid);
    }
}

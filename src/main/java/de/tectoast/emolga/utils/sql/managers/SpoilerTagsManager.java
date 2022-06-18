package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;

import static de.tectoast.emolga.commands.Command.spoilerTags;

public class SpoilerTagsManager extends DataManager {

    final LongColumn GUILDID = new LongColumn("guildid", this);

    public SpoilerTagsManager() {
        super("spoilertags");
        setColumns(GUILDID);
    }

    public boolean check(long guildid) {
        return GUILDID.isAny(guildid);
    }

    public boolean delete(long guildid) {
        return delete(GUILDID.check(guildid)) != 0;
    }

    public void addToList() {
        forAll(r -> spoilerTags.add(GUILDID.getValue(r)));
    }
}

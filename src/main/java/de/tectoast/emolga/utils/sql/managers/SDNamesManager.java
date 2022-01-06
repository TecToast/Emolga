package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.LongColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

public class SDNamesManager extends DataManager {

    final StringColumn NAME = new StringColumn("name", this);
    final LongColumn ID = new LongColumn("id", this);

    public SDNamesManager() {
        super("sdnames");
        setColumns(NAME, ID);
    }

    public long getIDByName(String name) {
        return ID.retrieveValue(NAME, Command.toUsername(name));
    }

    public void addIfAbsend(String name, long id) {
        if (!NAME.isAny(name)) {
            insert(Command.toUsername(name), id);
        }
    }


}

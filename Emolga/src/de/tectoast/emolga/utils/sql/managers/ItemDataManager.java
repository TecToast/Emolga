package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

public class ItemDataManager extends DataManager {

    final StringColumn NAME = new StringColumn("name", this);
    final StringColumn DESCRIPTION = new StringColumn("description", this);

    public ItemDataManager() {
        super("itemdata");
        setColumns(NAME, DESCRIPTION);
    }

    public String getData(String abiname) {
        return DESCRIPTION.retrieveValue(NAME, Command.toSDName(abiname));
    }
}

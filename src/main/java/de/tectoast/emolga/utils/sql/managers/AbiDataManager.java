package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

public class AbiDataManager extends DataManager {

    final StringColumn NAME = new StringColumn("name", this);
    final StringColumn DESCRIPTION = new StringColumn("description", this);

    public AbiDataManager() {
        super("abidata");
        setColumns(NAME, DESCRIPTION);
    }

    public String getData(String name) {
        return DESCRIPTION.retrieveValue(NAME, Command.toSDName(name));
    }
}

package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

public class StatisticsManager extends DataManager {

    final StringColumn NAME = new StringColumn("name", this);
    final IntColumn COUNT = new IntColumn("count", this);

    public StatisticsManager() {
        super("statistics");
        setColumns(NAME, COUNT);
    }

    public void increment(String name) {
        COUNT.increment(NAME, name);
    }
}

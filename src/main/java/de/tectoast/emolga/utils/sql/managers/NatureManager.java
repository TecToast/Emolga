package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

import java.util.HashMap;
import java.util.Map;

public class NatureManager extends DataManager {

    final StringColumn NAME = new StringColumn("name", this);
    final StringColumn PLUS = new StringColumn("plus", this);
    final StringColumn MINUS = new StringColumn("minus", this);

    final Map<String, String> statnames;

    public NatureManager() {
        super("natures");
        setColumns(NAME, PLUS, MINUS);
        statnames = new HashMap<>();
        statnames.put("atk", "Atk");
        statnames.put("def", "Def");
        statnames.put("spa", "SpAtk");
        statnames.put("spd", "SpDef");
        statnames.put("spe", "Init");
    }

    public String getNatureData(String str) {
        return read(selectAll(NAME.check(str)), s -> {
            return mapFirst(s, set -> {
                String plus = PLUS.getValue(set);
                String minus = MINUS.getValue(set);
                if (plus != null) {
                    return statnames.get(plus) + "+\n" + statnames.get(minus) + "-";
                } else {
                    return "Neutral";
                }
            }, null);
        });
    }
}

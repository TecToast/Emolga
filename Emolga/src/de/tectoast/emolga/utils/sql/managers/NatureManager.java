package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class NatureManager extends DataManager {

    final StringColumn NAME = new StringColumn("name", this);
    final StringColumn PLUS = new StringColumn("plus", this);
    final StringColumn MINUS = new StringColumn("minus", this);

    public NatureManager() {
        super("natures");
        setColumns(NAME, PLUS, MINUS);
    }

    public Pair<String, String> getNatureData(String str) {
        return read(select(NAME.check(str), PLUS, MINUS), r -> {
            r.next();
            return new ImmutablePair<>(PLUS.getValue(r), MINUS.getValue(r));
        });
    }
}

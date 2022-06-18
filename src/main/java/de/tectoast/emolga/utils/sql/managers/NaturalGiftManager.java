package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.records.NGData;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NaturalGiftManager extends DataManager {

    final StringColumn NAME = new StringColumn("name", this);
    final StringColumn TYPE = new StringColumn("type", this);
    final IntColumn BP = new IntColumn("bp", this);

    public NaturalGiftManager() {
        super("naturalgift");
        setColumns(NAME, TYPE, BP);
    }

    public @Nullable NGData fromName(String name) {
        return read(selectAll(NAME.check(name)), s -> {
            return mapFirst(s, set -> new NGData(name, TYPE.getValue(set), BP.getValue(set)), null);
        });
    }

    public @Nullable List<NGData> fromType(String type) {
        return read(selectAll(TYPE.check(type)), s -> {
            return map(s, set -> new NGData(NAME.getValue(set), type, BP.getValue(set)));
        });
    }
}

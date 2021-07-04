package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;

import java.sql.ResultSet;

public abstract class SQLColumn<T> {
    public final String name;
    public final DataManager manager;

    public SQLColumn(String name, DataManager manager) {
        this.name = name;
        this.manager = manager;
    }

    public String check(Object value) {
        return name + " = " + wrap(value);
    }

    public String wrap(Object value) {
        return value.toString();
    }

    public T retrieveValue(SQLColumn<?> checkcolumn, Object checkvalue) {
        return manager.read(manager.select(checkcolumn.check(checkvalue), this), rs -> {
            if (rs.next()) {
                return getValue(rs);
            }
            return null;
        });
    }

    public boolean isAny(Object value) {
        return retrieveValue(this, value) != null;
    }


    public abstract T getValue(ResultSet set);

    public abstract void update(ResultSet set, Object value);
}

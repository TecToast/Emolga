package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BooleanColumn extends SQLColumn<Boolean> {

    public BooleanColumn(String name, DataManager manager) {
        super(name, manager);
    }

    @Override
    public Boolean getValue(ResultSet set) {
        try {
            return set.getBoolean(name);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    @Override
    public void update(ResultSet set, Object value) {
        try {
            set.updateBoolean(name, (Boolean) value);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public Boolean retrieveValue(SQLColumn<?> checkcolumn, Object checkvalue) {
        return manager.read(manager.select(checkcolumn.check(checkvalue), this), rs -> {
            if (rs.next()) {
                return getValue(rs);
            }
            return false;
        });
    }

    @Override
    public String wrap(Object value) {
        return value.equals(true) ? "TRUE" : "FALSE";
    }
}

package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IntColumn extends SQLColumn<Integer> {

    public IntColumn(String name, DataManager manager) {
        super(name, manager);
    }

    @Override
    public Integer getValue(ResultSet set) {
        try {
            return set.getInt(name);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    @Override
    public void update(ResultSet set, Object value) {
        try {
            set.updateInt(name, (Integer) value);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public Integer retrieveValue(SQLColumn<?> checkcolumn, Object checkvalue) {
        return manager.read(manager.select(checkcolumn.check(checkvalue), this), rs -> {
            if (rs.next()) {
                return getValue(rs);
            }
            return -1;
        });
    }

    public void increment(SQLColumn<?> checkcolumn, Object checkvalue) {
        Integer val = retrieveValue(checkcolumn, checkvalue);
        if(val == null) val = 1;
        else val += 1;
        this.manager.editOneValue(checkcolumn, checkvalue, this, val);
    }
}

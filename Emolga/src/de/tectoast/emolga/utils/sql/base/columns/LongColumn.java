package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LongColumn extends SQLColumn<Long> {

    public LongColumn(String name, DataManager manager) {
        super(name, manager);
    }

    @Override
    public Long getValue(ResultSet set) {
        try {
            return set.getLong(name);
        } catch (SQLException throwables) {
            //throwables.printStackTrace();
        }
        return null;
    }

    @Override
    public void update(ResultSet set, Object value) {
        try {
            set.updateLong(name, (Long) value);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void increment(SQLColumn<?> checkcolumn, Object checkvalue) {
        Long val = retrieveValue(checkcolumn, checkvalue);
        if(val == null) val = 1L;
        else val += 1;
        this.manager.editOneValue(checkcolumn, checkvalue, this, val);
    }
}

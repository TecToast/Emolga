package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LongColumn extends SQLColumn<Long> {

    public LongColumn(String name, DataManager manager) {
        super(name, manager);
    }

    @Override
    public @Nullable Long getValue(ResultSet set) {
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

    @Override
    public Long retrieveValue(SQLColumn<?> checkcolumn, Object checkvalue) {
        return DataManager.read(manager.select(checkcolumn.check(checkvalue), this), rs -> {
            if (rs.next()) {
                return getValue(rs);
            }
            return -1L;
        });
    }

    public void increment(SQLColumn<?> checkcolumn, Object checkvalue) {
        this.manager.addStatisticsSpecified(checkvalue.toString(), List.of(checkcolumn, this), 1);
    }
}

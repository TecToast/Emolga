package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

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
    public Long retrieveValue(SQLColumn<?> checkcolumn, Object checkvalue) {
        return DataManager.read(manager.select(checkcolumn.check(checkvalue), this), rs -> {
            if (rs.next()) {
                return getValue(rs);
            }
            return -1L;
        });
    }

}

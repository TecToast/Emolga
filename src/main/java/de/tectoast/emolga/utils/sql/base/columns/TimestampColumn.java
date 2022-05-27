package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TimestampColumn extends SQLColumn<Timestamp> {
    public TimestampColumn(String name, DataManager manager) {
        super(name, manager);
    }

    @Override
    public @Nullable Timestamp getValue(ResultSet set) {
        try {
            return set.getTimestamp(name);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    @Override
    public void update(ResultSet set, Object value) {
        try {
            set.updateTimestamp(name, (Timestamp) value);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public String wrap(Object value) {
        return "\"" + value.toString() + "\"";
    }
}

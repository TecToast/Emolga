package de.tectoast.emolga.utils.sql.base.columns;

import de.tectoast.emolga.utils.sql.base.DataManager;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StringColumn extends SQLColumn<String> {

    public StringColumn(String name, DataManager manager) {
        super(name, manager);
    }

    @Override
    public String wrap(Object value) {
        if(value == null) return "NULL";
        return "\"" + value + "\"";
    }

    @Override
    public @Nullable String getValue(ResultSet set) {
        try {
            return set.getString(name);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    @Override
    public void update(ResultSet set, Object value) {
        try {
            set.updateString(name, (String) value);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}

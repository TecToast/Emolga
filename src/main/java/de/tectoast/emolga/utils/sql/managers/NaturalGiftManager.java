package de.tectoast.emolga.utils.sql.managers;

import de.tectoast.emolga.utils.records.NGData;
import de.tectoast.emolga.utils.sql.base.DataManager;
import de.tectoast.emolga.utils.sql.base.columns.IntColumn;
import de.tectoast.emolga.utils.sql.base.columns.StringColumn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class NaturalGiftManager extends DataManager {

    StringColumn NAME = new StringColumn("name", this);
    StringColumn TYPE = new StringColumn("type", this);
    IntColumn BP = new IntColumn("bp", this);

    public NaturalGiftManager() {
        super("naturalgift");
        setColumns(NAME, TYPE, BP);
    }

    public NGData fromName(String name) {
        ResultSet set = NAME.getSingle(name);
        try {
            return new NGData(name, set.getString("type"), set.getInt("bp"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<NGData> fromType(String type) {
        ResultSet set = TYPE.getAll(type);
        try {
            List<NGData> list = new LinkedList<>();
            while (set.next()) {
                list.add(new NGData(set.getString("name"), type, set.getInt("bp")));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet
import java.sql.Timestamp

class TimestampColumn(name: String, manager: DataManager) : SQLColumn<Timestamp?>(name, manager) {
    override fun getValue(set: ResultSet): Timestamp {
        return set.getTimestamp(name)
    }

    override fun wrap(value: Any?): String {
        return "\"" + value.toString() + "\""
    }
}
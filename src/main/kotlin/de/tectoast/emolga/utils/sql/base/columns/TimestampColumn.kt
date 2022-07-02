package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp

class TimestampColumn(name: String, manager: DataManager) : SQLColumn<Timestamp?>(name, manager) {
    override fun getValue(set: ResultSet): Timestamp {
        try {
            return set.getTimestamp(name)
        } catch (e: SQLException) {
            throw e
        }
    }

    override fun wrap(value: Any?): String {
        return "\"" + value.toString() + "\""
    }
}
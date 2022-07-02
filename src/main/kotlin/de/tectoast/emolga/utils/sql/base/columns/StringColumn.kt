package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet
import java.sql.SQLException

class StringColumn(name: String, manager: DataManager) : SQLColumn<String?>(name, manager) {
    override fun wrap(value: Any?): String {
        return if (value == null) "NULL" else "\"" + value + "\""
    }

    override fun getValue(set: ResultSet): String {
        try {
            return set.getString(name)
        } catch (e: SQLException) {
            throw e
        }
    }
}
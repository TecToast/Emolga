package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet

class StringColumn(name: String, manager: DataManager) : SQLColumn<String?>(name, manager) {
    override fun wrap(value: Any?): String {
        return if (value == null) "NULL" else "\"" + value + "\""
    }

    override fun getValue(set: ResultSet): String {
        return set.getString(name)
    }

    fun getNullableValue(set: ResultSet): String? {
        return set.getString(name)
    }
}
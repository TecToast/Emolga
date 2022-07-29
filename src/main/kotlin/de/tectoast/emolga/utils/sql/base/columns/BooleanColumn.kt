package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet

class BooleanColumn(name: String, manager: DataManager) : SQLColumn<Boolean>(name, manager) {
    override fun getValue(set: ResultSet): Boolean {
        return set.getBoolean(name)
    }

    override fun retrieveValue(checkcolumn: SQLColumn<*>, checkvalue: Any): Boolean? {
        return DataManager.read<Boolean?>(manager.select(checkcolumn.check(checkvalue), this)) { rs: ResultSet ->
            if (rs.next()) getValue(rs) else
                null
        }
    }

    override fun wrap(value: Any?): String {
        return if (value == true) "TRUE" else "FALSE"
    }
}
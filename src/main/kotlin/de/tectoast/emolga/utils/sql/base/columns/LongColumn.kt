package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet
import java.sql.SQLException

class LongColumn(name: String, manager: DataManager) : SQLColumn<Long?>(name, manager) {
    override fun getValue(set: ResultSet): Long {
        try {
            return set.getLong(name)
        } catch (e: SQLException) {
            throw e
        }
    }

    override fun retrieveValue(checkcolumn: SQLColumn<*>, checkvalue: Any): Long? {
        return DataManager.read<Long?>(manager.select(checkcolumn.check(checkvalue), this)) { rs: ResultSet ->
            if (rs.next()) {
                return@read getValue(rs)
            }
            null
        }
    }
}
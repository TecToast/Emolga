package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet

class IntColumn(name: String, manager: DataManager) : SQLColumn<Int?>(name, manager) {
    override fun getValue(set: ResultSet): Int {
        return set.getInt(name)
    }

    override fun retrieveValue(checkcolumn: SQLColumn<*>, checkvalue: Any): Int? {
        return DataManager.read<Int?>(manager.select(checkcolumn.check(checkvalue), this)) { rs: ResultSet ->
            if (rs.next())
                getValue(rs)
            else
                null
        }
    }

    fun increment(checkcolumn: SQLColumn<*>, checkvalue: Any) {
        manager.addStatisticsSpecified(checkvalue.toString(), listOf(checkcolumn, this), 1)
    }
}
package de.tectoast.emolga.utils.sql.base.columns

import de.tectoast.emolga.utils.sql.base.DataManager
import java.sql.ResultSet

abstract class SQLColumn<T>(val name: String, val manager: DataManager) {
    fun check(value: Any?): String {
        return name + " = " + wrap(value)
    }

    open fun wrap(value: Any?): String {
        return value?.toString() ?: "NULL"
    }

    open fun retrieveValue(checkcolumn: SQLColumn<*>, checkvalue: Any): T? {
        return DataManager.read<T?>(manager.select(checkcolumn.check(checkvalue), this)) { rs: ResultSet ->
            if (rs.next())
                return@read getValue(rs)
            else
                null
        }
    }

    fun getAll(value: T): ResultSet? {
        return DataManager.read<ResultSet?>(manager.selectAll(check(value))) { rs: ResultSet? -> rs }
    }

    fun isAny(value: Any): Boolean {
        return retrieveValue(this, value) != null
    }

    abstract fun getValue(set: ResultSet): T
}
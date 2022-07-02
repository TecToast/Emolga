package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.records.CalendarEntry
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn
import java.sql.ResultSet
import java.sql.Timestamp

object CalendarManager : DataManager("calendar") {
    private val MESSAGE = StringColumn("message", this)
    private val EXPIRES = TimestampColumn("expires", this)

    init {
        setColumns(MESSAGE, EXPIRES)
    }

    fun insertNewEntry(message: String, expires: Timestamp) {
        insert(message, expires)
    }

    val allEntries: List<CalendarEntry>
        get() = read(selectAll(), ResultsFunction { s ->
            map(s) { set: ResultSet ->
                CalendarEntry(
                    MESSAGE.getValue(
                        set
                    ), EXPIRES.getValue(set)
                )
            }
        })

    fun delete(expires: Timestamp) {
        delete(EXPIRES.check(expires))
    }
}
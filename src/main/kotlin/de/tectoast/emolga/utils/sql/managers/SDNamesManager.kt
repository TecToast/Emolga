package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.commands.Command.Companion.toUsername
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn

object SDNamesManager : DataManager("sdnames") {
    private val NAME = StringColumn("name", this)
    private val ID = LongColumn("id", this)

    init {
        setColumns(NAME, ID)
    }

    fun getIDByName(name: String): Long? {
        return ID.retrieveValue(NAME, toUsername(name))
    }

    fun addIfAbsent(name: String, id: Long): Boolean {
        if (!NAME.isAny(name)) {
            insert(toUsername(name), id)
            return true
        }
        return false
    }
}
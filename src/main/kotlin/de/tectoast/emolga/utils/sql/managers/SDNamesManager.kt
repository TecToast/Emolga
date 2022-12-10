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

    fun getIDByName(name: String): Long {
        return ID.retrieveValue(NAME, toUsername(name)) ?: -1
    }

    fun addIfAbsent(name: String, id: Long): Boolean {
        val username = toUsername(name)
        if (username.isEmpty()) return false
        if (!NAME.isAny(username)) {
            insert(username, id)
            return true
        }
        return false
    }
}

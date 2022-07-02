package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.commands.Command.Companion.toSDName
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.StringColumn

object ItemDataManager : DataManager("itemdata") {
    private val NAME = StringColumn("name", this)
    private val DESCRIPTION = StringColumn("description", this)

    init {
        setColumns(NAME, DESCRIPTION)
    }

    fun getData(name: String): String? {
        return DESCRIPTION.retrieveValue(NAME, toSDName(name))
    }
}
package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import java.sql.ResultSet

object SpoilerTagsManager : DataManager("spoilertags") {
    private val GUILDID = LongColumn("guildid", this)

    init {
        setColumns(GUILDID)
    }

    fun delete(guildid: Long): Boolean {
        return delete(GUILDID.check(guildid)) != 0
    }

    fun addToList() {
        forAll { r: ResultSet ->
            Command.spoilerTags.add(
                GUILDID.getValue(
                    r
                )
            )
        }
    }
}
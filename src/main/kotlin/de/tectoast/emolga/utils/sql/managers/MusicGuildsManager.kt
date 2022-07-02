package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn

object MusicGuildsManager : DataManager("musicguilds") {
    private val GUILDID = LongColumn("guildid", this)

    init {
        setColumns(GUILDID)
    }

    fun addGuild(gid: Long) {
        insert(gid)
    }
}
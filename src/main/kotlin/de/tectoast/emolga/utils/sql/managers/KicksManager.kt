package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn

object KicksManager : DataManager("kicks") {
    private val USERID = LongColumn("userid", this)
    private val MODID = LongColumn("modid", this)
    private val GUILDID = LongColumn("guildid", this)
    private val REASON = StringColumn("reason", this)
    private val TIMESTAMP = TimestampColumn("timestamp", this)

    init {
        setColumns(USERID, MODID, GUILDID, REASON, TIMESTAMP)
    }

    fun kick(userid: Long, modid: Long, guildid: Long, reason: String) {
        insert(userid, modid, guildid, reason, null)
    }
}
package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn

object ReplayCheckManager : DataManager("replaycheck") {
    private val CHANNELID = LongColumn("channelid", this)
    private val MESSAGEID = LongColumn("messageid", this)

    init {
        setColumns(CHANNELID, MESSAGEID)
    }

    operator fun set(tcid: Long, mid: Long) {
        replaceIfExists(tcid, mid)
    }
}
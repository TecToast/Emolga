package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.Giveaway
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.IntColumn
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn

object GiveawayManager : DataManager("giveaways") {
    private val MESSAGEID = LongColumn("messageid", this)
    private val CHANNELID = LongColumn("channelid", this)
    private val HOSTID = LongColumn("hostid", this)
    private val PRIZE = StringColumn("prize", this)
    private val END = TimestampColumn("end", this)
    private val WINNERS = IntColumn("winners", this)

    init {
        setColumns(MESSAGEID, CHANNELID, HOSTID, PRIZE, END, WINNERS)
    }

    fun saveGiveaway(g: Giveaway) {
        insert(g.messageId, g.channelId, g.userId, g.prize, g.end, g.winners)
    }

    fun removeGiveaway(g: Giveaway) {
        delete(MESSAGEID.check(g.messageId))
    }
}
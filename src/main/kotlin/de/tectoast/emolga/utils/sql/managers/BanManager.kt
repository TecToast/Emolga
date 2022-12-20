package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.Condition.and
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn
import java.sql.Timestamp

object BanManager : DataManager("bans") {
    private val USERID = LongColumn("userid", this)
    private val USERNAME = StringColumn("username", this)
    private val MODID = LongColumn("modid", this)
    private val GUILDID = LongColumn("guildid", this)
    private val REASON = StringColumn("reason", this)
    private val TIMESTAMP = TimestampColumn("timestamp", this)
    private val EXPIRES = TimestampColumn("expires", this)

    init {
        setColumns(USERID, USERNAME, MODID, GUILDID, REASON, TIMESTAMP, EXPIRES)
    }

    fun ban(userid: Long, username: String, modid: Long, guildid: Long, reason: String, expires: Timestamp?) {
        insert(userid, username, modid, guildid, reason, null, expires)
    }

    fun unban(userid: Long, guildid: Long): Int {
        return delete(and(USERID.check(userid), GUILDID.check(guildid)))
    }

}

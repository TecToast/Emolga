package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.Condition.and
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import de.tectoast.emolga.utils.sql.base.columns.TimestampColumn
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

object WarnsManager : DataManager("warns") {
    private val USERID = LongColumn("userid", this)
    private val MODID = LongColumn("modid", this)
    private val GUILDID = LongColumn("guildid", this)
    private val REASON = StringColumn("reason", this)
    private val TIMESTAMP = TimestampColumn("timestamp", this)

    init {
        setColumns(USERID, MODID, GUILDID, REASON, TIMESTAMP)
    }

    fun warn(userid: Long, modid: Long, guildid: Long, reason: String?) {
        insert(userid, modid, guildid, reason, null)
    }

    fun warnCount(userid: Long, guildid: Long): Int {
        return read(
            selectBuilder().count("warncount").where(and(USERID.check(userid), GUILDID.check(guildid))).build(this),
            ResultsFunction { s ->
                mapFirst(s) { set: ResultSet ->
                    unwrapCount(
                        set, "warncount"
                    )
                } ?: 0
            })
    }

    fun getWarnsFrom(userid: Long, guildid: Long): String {
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm")
        return read(selectAll(and(USERID.check(userid), GUILDID.check(guildid))), ResultsFunction { set ->
            map(set) { s: ResultSet ->
                "Von: <@${MODID.getValue(s)}>\nGrund: ${REASON.getValue(s)}\nZeitpunkt: ${
                    format.format(
                        Date(
                            TIMESTAMP.getValue(s).time
                        )
                    )
                } Uhr"
            }.joinToString(separator = "\n\n")
        })
    }
}

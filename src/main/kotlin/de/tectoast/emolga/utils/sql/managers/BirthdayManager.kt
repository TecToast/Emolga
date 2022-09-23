package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.utils.sql.base.Condition.and
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.IntColumn
import de.tectoast.emolga.utils.sql.base.columns.LongColumn
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.sql.ResultSet
import java.util.*

object BirthdayManager : DataManager("birthdays") {
    private val USERID = LongColumn("userid", this)
    private val YEAR = IntColumn("year", this)
    private val MONTH = IntColumn("month", this)
    private val DAY = IntColumn("day", this)

    init {
        setColumns(USERID, YEAR, MONTH, DAY)
    }

    fun addOrUpdateBirthday(userid: Long, year: Int, month: Int, day: Int) {
        //insertOrUpdate(USERID, userid, userid, year, month, day);
        replaceIfExists(userid, year, month, day)
    }

    fun checkBirthdays(c: Calendar, tc: MessageChannel) {
        read(selectAll(and(MONTH.check(c[Calendar.MONTH] + 1), DAY.check(c[Calendar.DAY_OF_MONTH])))) { s ->
            forEach(s) { set: ResultSet ->
                tc.sendMessage(
                    "Alles Gute zum " + (Calendar.getInstance()[Calendar.YEAR] - YEAR.getValue(
                        set
                    )) + ". Geburtstag, <@" + USERID.getValue(set) + ">!"
                ).queue()
            }
        }
    }

    val all: List<Data>
        get() = read(selectAll(), ResultsFunction { set ->
            map(set) { s: ResultSet ->
                Data(
                    USERID.getValue(
                        s
                    ), MONTH.getValue(s), DAY.getValue(s)
                )
            }
        })

    class Data(val userid: Long, val month: Int, val day: Int)
}
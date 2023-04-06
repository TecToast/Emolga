package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.upsert
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object BirthdayDB : Table("birthdays") {
    val USERID = long("userid")
    val YEAR = integer("year")
    val MONTH = integer("month")
    val DAY = integer("day")
    override val primaryKey = PrimaryKey(USERID)

    fun addOrUpdateBirthday(userid: Long, year: Int, month: Int, day: Int) = transaction {
        upsert(userid, mapOf(YEAR to year, MONTH to month, DAY to day))
    }

    fun checkBirthdays(c: Calendar, tc: MessageChannel) {
        transaction {
            select {
                MONTH eq (c[Calendar.MONTH] + 1) and (DAY eq c[Calendar.DAY_OF_MONTH])
            }.forEach {
                tc.sendMessage("Alles Gute zum ${c[Calendar.YEAR] - it[YEAR]}. Geburtstag, <@${it[USERID]}>!").queue()
            }
        }
    }

    val all: List<Data>
        get() = transaction {
            selectAll().map {
                Data(it[USERID], it[MONTH], it[DAY])
            }
        }

    class Data(val userid: Long, val month: Int, val day: Int)
}

package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.upsert
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

object BirthdayDB : Table("birthdays") {
    val USERID = long("userid")
    val YEAR = integer("year")
    val MONTH = integer("month")
    val DAY = integer("day")
    override val primaryKey = PrimaryKey(USERID)

    suspend fun addOrUpdateBirthday(userid: Long, year: Int, month: Int, day: Int) = newSuspendedTransaction {
        upsert(userid, mapOf(YEAR to year, MONTH to month, DAY to day))
    }

    suspend fun checkBirthdays(c: Calendar, tc: MessageChannel) {
        newSuspendedTransaction {
            select {
                MONTH eq (c[Calendar.MONTH] + 1) and (DAY eq c[Calendar.DAY_OF_MONTH])
            }.forEach {
                val age = if (it[USERID] == Constants.M.TARIA) 17 else c[Calendar.YEAR] - it[YEAR]
                tc.sendMessage("Alles Gute zum $age. Geburtstag, <@${it[USERID]}>!").queue()
            }
        }
    }
}

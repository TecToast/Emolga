package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import java.util.*

object BirthdayDB : Table("birthdays") {
    val USERID = long("userid")
    val YEAR = integer("year")
    val MONTH = integer("month")
    val DAY = integer("day")
    override val primaryKey = PrimaryKey(USERID)

    /**
     * Upserts a birthday for a user
     * @param userid the user id
     * @param year the year of birth
     * @param month the month of birth
     * @param day the day of birth
     */
    suspend fun upsertBirthday(userid: Long, year: Int, month: Int, day: Int) {
        newSuspendedTransaction {
            upsert {
                it[USERID] = userid
                it[YEAR] = year
                it[MONTH] = month
                it[DAY] = day
            }
        }
    }

    /**
     * Checks if there are any birthdays today and sends a message to the specified channel
     * @param cal the calendar to check against
     * @param tc the channel to send the message to
     */
    suspend fun checkBirthdays(cal: Calendar, tc: MessageChannel) {
        newSuspendedTransaction {
            selectAll().where {
                MONTH eq (cal[Calendar.MONTH] + 1) and (DAY eq cal[Calendar.DAY_OF_MONTH])
            }.forEach {
                val age = if (it[USERID] == Constants.M.TARIA) 17 else cal[Calendar.YEAR] - it[YEAR]
                tc.sendMessage("Alles Gute zum $age. Geburtstag, <@${it[USERID]}>!").queue()
            }
        }
    }
}

package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.translateToGuildLanguage
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
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
        dbTransaction {
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
    suspend fun checkBirthdays(cal: Calendar, tc: TextChannel) {
        dbTransaction {
            selectAll().where {
                MONTH eq (cal[Calendar.MONTH] + 1) and (DAY eq cal[Calendar.DAY_OF_MONTH])
            }.collect {
                val age = if (it[USERID] == 322755315953172485) 17 else cal[Calendar.YEAR] - it[YEAR]
                tc.sendMessage(K18n_BirthdayGratulation(age, it[USERID]).translateToGuildLanguage(tc.guild.idLong))
                    .queue()

            }
        }
    }
}

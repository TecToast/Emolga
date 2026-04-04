package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single

data class Birthday(val userId: Long, val year: Int)

@Single
class BirthdayRepository(val db: R2dbcDatabase) {
    /**
     * Sets the BirthdayTable of a user
     * @param userId the user
     * @param year the year
     * @param month the month
     * @param day the day
     */
    suspend fun set(userId: Long, year: Int, month: Int, day: Int) {
        suspendTransaction(db) {
            BirthdayTable.upsert {
                it[BirthdayTable.userid] = userId
                it[BirthdayTable.year] = year
                it[BirthdayTable.month] = month
                it[BirthdayTable.day] = day
            }
        }
    }

    /**
     * Gets BirthdayTables matching the given month and day.
     * @param month the month
     * @param day the day
     * @return a list of BirthdayTables matching the given month and day
     */
    suspend fun getBirthdays(
        month: Int,
        day: Int
    ): List<Birthday> {
        return suspendTransaction(db) {
            BirthdayTable.select(BirthdayTable.userid, BirthdayTable.year).where {
                BirthdayTable.month eq month and (BirthdayTable.day eq day)
            }.map {
                Birthday(it[BirthdayTable.userid], it[BirthdayTable.year])
            }.toList()
        }
    }
}


object BirthdayTable : Table("BirthdayTables") {
    val userid = long("userid")
    val year = integer("year")
    val month = integer("month")
    val day = integer("day")

    override val primaryKey = PrimaryKey(userid)
}

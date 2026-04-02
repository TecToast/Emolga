package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
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

interface BirthdayRepository {
    /**
     * Sets the birthday of a user
     * @param userId the user
     * @param year the year
     * @param month the month
     * @param day the day
     */
    suspend fun set(userId: Long, year: Int, month: Int, day: Int)

    /**
     * Gets birthdays matching the given month and day.
     * @param month the month
     * @param day the day
     * @return a list of birthdays matching the given month and day
     */
    suspend fun getBirthdays(month: Int, day: Int): List<Birthday>
}

@Single
class PostgresBirthdayRepository(val db: R2dbcDatabase, val birthday: BirthdayDB) : BirthdayRepository {
    override suspend fun set(userId: Long, year: Int, month: Int, day: Int) {
        suspendTransaction(db) {
            birthday.upsert {
                it[birthday.userid] = userId
                it[birthday.year] = year
                it[birthday.month] = month
                it[birthday.day] = day
            }
        }
    }

    override suspend fun getBirthdays(
        month: Int,
        day: Int
    ): List<Birthday> {
        return dbTransaction {
            birthday.select(birthday.userid, birthday.year).where {
                birthday.month eq month and (birthday.day eq day)
            }.map {
                Birthday(it[birthday.userid], it[birthday.year])
            }.toList()
        }
    }
}

@Single
class BirthdayDB : Table("birthdays") {
    val userid = long("userid")
    val year = integer("year")
    val month = integer("month")
    val day = integer("day")

    override val primaryKey = PrimaryKey(userid)
}

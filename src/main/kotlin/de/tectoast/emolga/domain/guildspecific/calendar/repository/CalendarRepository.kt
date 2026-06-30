@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.domain.guildspecific.calendar.repository

import de.tectoast.emolga.domain.guildspecific.calendar.model.CalendarEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@Single
class CalendarRepository(private val db: R2dbcDatabase) {


    suspend fun createNewCalendarEntry(message: String, expires: Instant) = suspendTransaction(db) {
        CalendarTable.insertReturning {
            it[CalendarTable.message] = message
            it[CalendarTable.expires] = expires
        }.first()[CalendarTable.id]
    }

    suspend fun doesntExist(id: Int): Boolean = suspendTransaction(db) {
        CalendarTable.select(CalendarTable.id).where { CalendarTable.id eq id }.count() == 0L
    }

    suspend fun removeEntry(id: Int) {
        suspendTransaction(db) {
            CalendarTable.deleteWhere { CalendarTable.id eq id }
        }
    }

    suspend fun getAllEntries() = suspendTransaction(db) {
        CalendarTable.selectAll()
            .orderBy(CalendarTable.expires to SortOrder.ASC)
            .map { CalendarEntry(it[CalendarTable.id], it[CalendarTable.message], it[CalendarTable.expires]) }
            .toList()
    }
}


object CalendarTable : Table("calendar") {
    val id = integer("id").autoIncrement()
    val message = text("message")
    val expires = timestamp("expires")

    override val primaryKey = PrimaryKey(id)
}

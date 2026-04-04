@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CalendarEntry(val id: Int, val message: String, val expires: Instant)
interface CalendarRepository {
    /**
     * Schedules a CalendarTable entry
     * @param message the message to send
     * @param expires the timestamp in milliseconds when the message should be sent
     * @return the id of the new entry
     */
    suspend fun createNewCalendarEntry(message: String, expires: Instant): Int


    suspend fun doesntExist(id: Int): Boolean
    suspend fun removeEntry(id: Int)
    suspend fun getAllEntries(): List<CalendarEntry>
}

class PostgresCalendarRepository(val db: R2dbcDatabase) : CalendarRepository {


    override suspend fun createNewCalendarEntry(message: String, expires: Instant) = suspendTransaction(db) {
        CalendarTable.insertReturning {
            it[CalendarTable.message] = message
            it[CalendarTable.expires] = expires
        }.first()[CalendarTable.id]
    }

    override suspend fun doesntExist(id: Int): Boolean = suspendTransaction(db) {
        CalendarTable.select(CalendarTable.id).where { CalendarTable.id eq id }.count() == 0L
    }

    override suspend fun removeEntry(id: Int) {
        suspendTransaction(db) {
            CalendarTable.deleteWhere { CalendarTable.id eq id }
        }
    }

    override suspend fun getAllEntries() = suspendTransaction(db) {
        CalendarTable.selectAll()
            .orderBy(CalendarTable.expires to SortOrder.ASC)
            .map { CalendarEntry(it[CalendarTable.id], it[CalendarTable.message], it[CalendarTable.expires]) }
            .toList()
    }
}


object CalendarTable : Table("CalendarTable") {
    val id = integer("id").autoIncrement()
    val message = text("message")
    val expires = timestamp("expires").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}

suspend fun <T> Flow<T>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, transform).toString()
}

suspend fun <T> Flow<T>.joinTo(
    buffer: StringBuilder,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    transform: ((T) -> CharSequence)? = null
): StringBuilder {
    val transformer = transform ?: { it.toString() }
    buffer.append(prefix)
    var count = 0
    collect { element ->
        if (++count > 1) buffer.append(separator)
        buffer.append(transformer(element))
    }
    buffer.append(postfix)
    return buffer
}

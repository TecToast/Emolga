@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.database.exposed

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import java.text.SimpleDateFormat
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CalendarEntry(val id: Int, val message: String, val expires: Instant)
interface CalendarRepository {
    /**
     * Schedules a calendar entry
     * @param message the message to send
     * @param expires the timestamp in milliseconds when the message should be sent
     * @return the id of the new entry
     */
    suspend fun createNewCalendarEntry(message: String, expires: Instant): Int

    /**
     * Retrieves all calendar entries
     */
    suspend fun buildCalendar(): String
    suspend fun doesntExist(id: Int): Boolean
    suspend fun removeEntry(id: Int)
    suspend fun getAllEntries(): List<CalendarEntry>
}

class PostgresCalendarRepository(val db: R2dbcDatabase, val calendar: CalendarDB) : CalendarRepository {
    private val calendarFormat = SimpleDateFormat("dd.MM. HH:mm")

    override suspend fun createNewCalendarEntry(message: String, expires: Instant) = suspendTransaction(db) {
        calendar.insertReturning {
            it[calendar.message] = message
            it[calendar.expires] = expires
        }.first()[calendar.id]
    }

    override suspend fun buildCalendar() = suspendTransaction(db) {
        calendar.selectAll().orderBy(calendar.expires)
            .joinToString("\n") { "**${calendarFormat.format(it[calendar.expires].toEpochMilliseconds())}:** ${it[calendar.message]}" }
            .ifEmpty { "_leer_" }
    }

    override suspend fun doesntExist(id: Int): Boolean = suspendTransaction(db) {
        calendar.select(calendar.id).where { calendar.id eq id }.count() == 0L
    }

    override suspend fun removeEntry(id: Int) {
        suspendTransaction(db) {
            calendar.deleteWhere { calendar.id eq id }
        }
    }

    override suspend fun getAllEntries() = suspendTransaction(db) {
        calendar.selectAll()
            .map { CalendarEntry(it[calendar.id], it[calendar.message], it[calendar.expires]) }
            .toList()
    }
}

@Single
class CalendarDB : Table("calendar") {
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

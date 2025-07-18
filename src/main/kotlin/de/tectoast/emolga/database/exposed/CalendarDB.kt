package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.various.CalendarSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.text.SimpleDateFormat

object CalendarDB : IntIdTable("calendar") {
    val MESSAGE = text("message")
    val EXPIRES = timestamp("expires").defaultExpression(CurrentTimestamp)

    private val calendarFormat = SimpleDateFormat("dd.MM. HH:mm")

    /**
     * Schedules a calendar entry
     * @param message the message to send
     * @param expires the timestamp in milliseconds when the message should be sent
     */
    suspend fun createNewCalendarEntry(message: String, expires: Long) = dbTransaction {
        val instant = Instant.fromEpochMilliseconds(expires)
        val newEntry = insertAndGetId {
            it[MESSAGE] = message
            it[EXPIRES] = instant
        }
        CalendarSystem.scheduleCalendarEntry(newEntry.value, message, instant)
    }

    suspend fun init() {
        dbTransaction {
            selectAll().collect { CalendarSystem.scheduleCalendarEntry(it[super.id].value, it[MESSAGE], it[EXPIRES]) }
        }
    }

    /**
     * Retrieves all calendar entries
     */
    suspend fun buildCalendar() = dbTransaction {
        selectAll().orderBy(EXPIRES)
            .joinToString("\n") { "**${calendarFormat.format(it[EXPIRES].toEpochMilliseconds())}:** ${it[MESSAGE]}" }
            .ifEmpty { "_leer_" }
    }

    suspend fun doesntExist(id: Int): Boolean = dbTransaction {
        select(super.id).where { super.id eq id }.count() == 0L
    }
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

suspend fun <T, A : Appendable> Flow<T>.joinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    transform: ((T) -> CharSequence)? = null
): A {
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
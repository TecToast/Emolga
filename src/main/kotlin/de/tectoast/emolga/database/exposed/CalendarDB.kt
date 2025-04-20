package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.various.CalendarSystem
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object CalendarDB : IntIdTable("calendar") {
    val MESSAGE = text("message")
    val EXPIRES = timestamp("expires").defaultExpression(CurrentTimestamp)


    /**
     * Schedules a calendar entry
     * @param message the message to send
     * @param expires the timestamp in milliseconds when the message should be sent
     */
    suspend fun scheduleCalendarEntry(message: String, expires: Long) =
        dbTransaction {
            CalendarSystem.scheduleCalendarEntry(CalendarEntry.new {
                this.message = message
                this.expires = Instant.fromEpochMilliseconds(expires / 1000 * 1000)
            })
        }

    /**
     * Retrieves all calendar entries
     */
    suspend fun getAllEntries() = dbTransaction { CalendarEntry.all().toList() }
}

class CalendarEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CalendarEntry>(CalendarDB)

    var message by CalendarDB.MESSAGE
    var expires by CalendarDB.EXPIRES
}

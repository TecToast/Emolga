package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.features.various.CalendarSystem
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant

object CalendarDB : IntIdTable("calendar") {
    val MESSAGE = varchar("message", 1000)
    val EXPIRES = timestamp("expires")

    suspend fun scheduleCalendarEntry(message: String, expires: Long) =
        newSuspendedTransaction {
            CalendarSystem.scheduleCalendarEntry(CalendarEntry.new {
                this.message = message
                this.expires = Instant.ofEpochMilli(expires / 1000 * 1000)
            })
        }

    suspend fun getAllEntries() = newSuspendedTransaction { CalendarEntry.all().toList() }
}

class CalendarEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CalendarEntry>(CalendarDB)

    var message by CalendarDB.MESSAGE
    var expires by CalendarDB.EXPIRES
}

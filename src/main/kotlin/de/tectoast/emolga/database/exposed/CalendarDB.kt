package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object CalendarDB : IntIdTable("calendar") {
    val MESSAGE = varchar("message", 1000)
    val EXPIRES = timestamp("expires")

    fun scheduleCalendarEntry(message: String, expires: Long) =
        transaction {
            Command.scheduleCalendarEntry(CalendarEntry.new {
                this.message = message
                this.expires = Instant.ofEpochMilli(expires / 1000 * 1000)
            })
        }

    val allEntries: List<CalendarEntry>
        get() = transaction {
            CalendarEntry.all().toList()
        }
}

class CalendarEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CalendarEntry>(CalendarDB)

    var message by CalendarDB.MESSAGE
    var expires by CalendarDB.EXPIRES
}

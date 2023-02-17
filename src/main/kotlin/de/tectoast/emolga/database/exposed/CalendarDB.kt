package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.remind.Person
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object CalendarDB : IntIdTable("calendar") {
    val message = varchar("message", 1000)
    val expires = timestamp("expires")
    val person = varchar("person", 1).nullable()
    val messageid = long("messageid").nullable()

    fun scheduleCalendarEntry(message: String, expires: Long, person: Person? = null, messageid: Long? = null) =
        transaction {
            Command.scheduleCalendarEntry(CalendarEntry.new {
                this.message = message
                this.expires = Instant.ofEpochMilli(expires / 1000 * 1000)
                this.person = person
                this.messageid = messageid
            })
        }

    val allFloEntries: List<CalendarEntry>
        get() = transaction {
            CalendarEntry.find { person.isNull() }.toList()
        }

    val allEntries: List<CalendarEntry>
        get() = transaction {
            CalendarEntry.all().toList()
        }
}

class CalendarEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CalendarEntry>(CalendarDB)

    var message by CalendarDB.message
    var expires by CalendarDB.expires
    var person: Person? by CalendarDB.person.transform(
        { it?.name },
        { it?.let { p -> Person.values().first { e -> e.name.equals(p, ignoreCase = true) } } })
    var messageid by CalendarDB.messageid
}

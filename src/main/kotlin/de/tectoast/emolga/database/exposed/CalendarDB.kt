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
    val MESSAGE = varchar("message", 1000)
    val EXPIRES = timestamp("expires")
    val PERSON = varchar("person", 1).nullable()
    val MESSAGEID = long("messageid").nullable()

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
            CalendarEntry.find { PERSON.isNull() }.toList()
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
    var person: Person? by CalendarDB.PERSON.transform(
        { it?.name },
        { it?.let { p -> Person.entries.first { e -> e.name.equals(p, ignoreCase = true) } } })
    var messageid by CalendarDB.MESSAGEID
}

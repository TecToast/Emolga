package de.tectoast.emolga.commands.remind

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.remind.Subject.*
import de.tectoast.emolga.database.exposed.CalendarDB
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.primary
import net.dv8tion.jda.api.entities.emoji.Emoji
import java.util.Calendar.*

class HomeworkCommand : AbstractRemindCommand("homework", "Hausaufgabe hinzugefügt!") {
    private fun getHomeworkMillis(subject: Subject): Map<Person, Long> {
        return buildMap {
            if (subject.f)
                put(Person.F, System.currentTimeMillis()
                    .let { now -> flodates.map { it.nextAppearance() }.filter { it - now > 0 }.min() })
            if (subject.t) {
                val calendar = getInstance()
                var counter = 0
                while (true) {
                    calendar.add(DAY_OF_WEEK, 1)
                    if (subjects[calendar[DAY_OF_WEEK]]?.contains(subject) == true) {
                        calendar[MINUTE] = 0
                        calendar[SECOND] = 0
                        calendar[HOUR_OF_DAY] = 15
                        calendar.add(DAY_OF_WEEK, -1)
                        break
                    }
                    counter++
                    if (counter > 30) error("too many iterations")
                }
                put(Person.T, calendar.timeInMillis)
            }
        }
    }

    override suspend fun process(e: GuildCommandEvent) {
        try {
            val args = e.arguments
            val subject = Subject.byName(args.getText("subject"))
            val expiresMap = getHomeworkMillis(subject)
            val message = args.getText("text")
            expiresMap.entries.forEach { (person, expires) ->
                CalendarDB.scheduleCalendarEntry(
                    "$subject $message",
                    expires,
                    person,
                    e.jda.getTextChannelById(person.tcid)!!
                        .sendMessage("**${calendarFormat.format(expires)} / ${subject.name}:** $message")
                        .addActionRow(primary("homework;done", "Gemacht", emoji = Emoji.fromUnicode("✅")))
                        .await().idLong
                )
            }
            e.reply("Hausaufgabe hinzugefügt!", ephemeral = true)
        } catch (ex: Exception) {
            e.reply("Es ist ein Fehler aufgetreten!", ephemeral = true)
            ex.printStackTrace()
        }
    }

    override fun getMillis(e: GuildCommandEvent): Long {
        return -1 // not being used because of the custom process method
    }

    override fun generateArgumentTemplate(): ArgumentManagerTemplate {
        return ArgumentManagerTemplate.create {
            add(
                "subject",
                "Fach",
                "Das Schulfach",
                ArgumentManagerTemplate.Text.of(Subject.allSubjects.map { SubCommand(it, it) })
            )
            add("text", "Text", "Der Text", ArgumentManagerTemplate.Text.any())
        }
    }

    companion object {
        val flodates = listOf(
            RemindDate(MONDAY, 11, 5), RemindDate(WEDNESDAY, 9, 30), RemindDate(THURSDAY, 11, 5)
        )
        val subjects = mapOf(
            MONDAY to listOf(PhysikL, BioL, Englisch, ChemieL, Musik, Kunst),
            TUESDAY to listOf(Deutsch, Geschichte, MatheL, PhysikL, BioL, Religion, Gemeinschaftskunde),
            WEDNESDAY to listOf(ChemieL, Physik, PhysikL, BioL),
            THURSDAY to listOf(MatheL, Deutsch, Englisch, Physik, Informatik),
            FRIDAY to listOf(ChemieL, MatheL, Sport)
        )
    }

}

enum class Subject(private val person: String) {
    PhysikL("f"), BioL("t"), Englisch("b"), ChemieL("b"), Musik("f"), Kunst("t"), Deutsch("b"), Geschichte("b"), MatheL(
        "b"
    ),
    Religion("b"), Gemeinschaftskunde("b"), Physik("t"), Informatik("f"), Sport("t");

    val f get() = person == "f" || person == "b"
    val t get() = person == "t" || person == "b"

    companion object {
        val allSubjects by lazy { values().map { it.name }.toSet() }
        fun byName(name: String) = values().first { it.name == name }
    }
}

enum class Person(val uid: Long, val tcid: Long) {
    F(Constants.FLOID, 1072622715657265313), T(637737732558553089, 1072622768245452820)

}

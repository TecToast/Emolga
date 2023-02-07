package de.tectoast.emolga.commands.remind

import java.util.Calendar.*

class HomeworkCommand : DatedRemindCommand(
    "homework", "Hausaufgabe geadded!", setOf(
        RemindDate(MONDAY, 11, 5),
        RemindDate(WEDNESDAY, 9, 30),
        RemindDate(THURSDAY, 11, 5)
    )
)

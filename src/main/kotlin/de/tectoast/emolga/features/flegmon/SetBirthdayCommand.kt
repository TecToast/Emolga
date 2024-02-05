package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.BirthdayDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.utils.Constants

object SetBirthdayCommand : CommandFeature<SetBirthdayCommand.Args>(
    ::Args,
    CommandSpec("setbirthday", "Setzt einen Geburtstag, damit ich dir gratulieren kann :)", Constants.G.PEPE)
) {
    class Args : Arguments() {
        var day by int("day", "Der Tag des Geburtstags")
        var month by int("month", "Der Monat des Geburtstags")
        var year by int("year", "Das Jahr des Geburtstags")
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val year = e.year
        val month = e.month
        val day = e.day
        BirthdayDB.addOrUpdateBirthday(user, year, month, day)
        reply(
            "Dein Geburtstag wurde erfolgreich auf den ${day.toString().padStart(2, '0')}.${
                month.toString().padStart(2, '0')
            }.$year gesetzt!"
        )
    }
}

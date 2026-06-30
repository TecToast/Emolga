package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.domain.guildspecific.flegmon.birthday.repository.BirthdayRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class SetBirthdayCommand(private val birthdayRepository: BirthdayRepository) : CommandFeature<SetBirthdayCommand.Args>(
    ::Args,
    CommandSpec("setbirthday", "Setzt einen Geburtstag, damit ich dir gratulieren kann :)".k18n)
) {
    class Args : Arguments() {
        var day by int("day", "Der Tag des Geburtstags".k18n)
        var month by int("month", "Der Monat des Geburtstags".k18n)
        var year by int("year", "Das Jahr des Geburtstags".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val year = e.year
        val month = e.month
        val day = e.day
        birthdayRepository.set(iData.user, year, month, day)
        iData.replyRaw(
            "Dein Geburtstag wurde erfolgreich auf den ${day.toString().padStart(2, '0')}.${
                month.toString().padStart(2, '0')
            }.$year gesetzt!"
        )
    }
}

package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.database.exposed.BirthdayDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.createCoroutineContext
import de.tectoast.emolga.utils.k18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

object BirthdaySystem : CoroutineScope {
    override val coroutineContext = createCoroutineContext("BirthdaySystem")
    fun startSystem() {
        EmolgaMain.flegmonjda ?: return
        launch {
            while (true) {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_MONTH, 1)
                c[Calendar.HOUR_OF_DAY] = 0
                c[Calendar.MINUTE] = 0
                c[Calendar.SECOND] = 0
                val tilnextday = c.timeInMillis - System.currentTimeMillis() + 1000
                delay(tilnextday)
                BirthdayDB.checkBirthdays(c, EmolgaMain.flegmonjda!!.getTextChannelById(605650587329232896L)!!)
            }
        }
    }

    object SetBirthdayCommand : CommandFeature<SetBirthdayCommand.Args>(
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
            BirthdayDB.upsertBirthday(iData.user, year, month, day)
            iData.reply(
                "Dein Geburtstag wurde erfolgreich auf den ${day.toString().padStart(2, '0')}.${
                    month.toString().padStart(2, '0')
                }.$year gesetzt!"
            )
        }
    }
}

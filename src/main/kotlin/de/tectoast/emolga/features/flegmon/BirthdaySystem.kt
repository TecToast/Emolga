package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.database.exposed.BirthdayRepository
import de.tectoast.emolga.database.exposed.K18n_BirthdayGratulation
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.k18n
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import kotlinx.coroutines.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class DayMonthYear(val day: Int, val month: Int, val year: Int)
interface DateTimeProvider {
    fun getDayMonthYear(): DayMonthYear
    fun getTimeUntilNextDay(): Duration
}

@Single
class CalendarDateTimeProvider : DateTimeProvider {
    override fun getDayMonthYear(): DayMonthYear {
        val cal = Calendar.getInstance()
        return DayMonthYear(cal[Calendar.DAY_OF_MONTH], cal[Calendar.MONTH] + 1, cal[Calendar.YEAR])
    }

    override fun getTimeUntilNextDay(): Duration {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_MONTH, 1)
        c[Calendar.HOUR_OF_DAY] = 0
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        return (c.timeInMillis - System.currentTimeMillis() + 1000).milliseconds
    }
}

@Single
class FlegmonBirthdayService(
    val repository: BirthdayRepository,
    val dateTimeProvider: DateTimeProvider,
    dispatcher: CoroutineDispatcher,
    @Named("flegmonBirthdaySender") val sender: de.tectoast.emolga.utils.MessageSender
) : StartupTask {
    private val scope = createCoroutineScope("FlegmonBirthdayService", dispatcher)
    private var job: Job? = null

    suspend fun checkBirthdays(sender: de.tectoast.emolga.utils.MessageSender) {
        val (day, month, year) = dateTimeProvider.getDayMonthYear()
        val birthdays = repository.getBirthdays(month, day)
        for (birthday in birthdays) {
            val age = if (birthday.userId == 322755315953172485) 17 else year - birthday.year
            sender.sendMessage(K18n_BirthdayGratulation(age, birthday.userId).translateTo(K18N_DEFAULT_LANGUAGE))
        }
    }

    override suspend fun onStartup() {
        startSystem()
    }

    fun startSystem() {
        job?.cancel() // Cancel existing job if started multiple times
        job = scope.launch {
            while (isActive) {
                val tilnextday = dateTimeProvider.getTimeUntilNextDay()
                delay(tilnextday)
                checkBirthdays(sender)
            }
        }
    }

    fun stopSystem() {
        job?.cancel()
        job = null
    }
}

@Single(binds = [ListenerProvider::class])
class SetBirthdayCommand(val birthdayRepository: BirthdayRepository) : CommandFeature<SetBirthdayCommand.Args>(
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
        iData.reply(
            "Dein Geburtstag wurde erfolgreich auf den ${day.toString().padStart(2, '0')}.${
                month.toString().padStart(2, '0')
            }.$year gesetzt!"
        )
    }
}

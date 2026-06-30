package de.tectoast.emolga.domain.guildspecific.flegmon.birthday.service

import de.tectoast.emolga.database.exposed.K18n_BirthdayGratulation
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.discord.MessageSender
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.guildspecific.flegmon.birthday.repository.BirthdayRepository
import de.tectoast.emolga.domain.guildspecific.flegmon.birthday.service.datetime.DateTimeProvider
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import kotlinx.coroutines.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class FlegmonBirthdayService(
    private val repository: BirthdayRepository,
    private val dateTimeProvider: DateTimeProvider,
    @Named("flegmonBirthdaySender") private val sender: MessageSender,
    dispatcher: CoroutineDispatcher,
) : StartupTask {
    private val scope = createCoroutineScope("FlegmonBirthdayService", dispatcher)
    private var job: Job? = null

    private suspend fun checkBirthdays() {
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

    private fun startSystem() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val tilnextday = dateTimeProvider.getTimeUntilNextDay()
                delay(tilnextday)
                checkBirthdays()
            }
        }
    }
}
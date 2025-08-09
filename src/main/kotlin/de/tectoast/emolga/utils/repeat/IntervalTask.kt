@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.utils.repeat

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.ktor.setupYTSuscribtions
import de.tectoast.emolga.utils.createCoroutineContext
import de.tectoast.emolga.utils.json.IntervalTaskData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.repeat.IntervalTaskKey.Snips
import de.tectoast.emolga.utils.repeat.IntervalTaskKey.YTSubscriptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litote.kmongo.eq
import org.litote.kmongo.upsert
import java.util.*
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class IntervalTask(
    val name: IntervalTaskKey,
    val provideNextExecution: () -> Instant,
    val consumer: suspend () -> Unit
) {
    constructor(name: IntervalTaskKey, delay: Duration, consumer: suspend () -> Unit) : this(
        name,
        { Clock.System.now() + delay },
        consumer
    )

    lateinit var job: Job

    init {
        intervalTasks[name] = this
        start()
    }

    fun start() {
        job = launch {
            val startData = db.intervaltaskdata.get(name)
            startData?.notAfter?.let {
                if (Clock.System.now() > it) return@launch
            }
            val now = Clock.System.now()
            delay((startData?.nextExecution ?: now) - now)
            consumer()
            while (true) {
                val nextLastExecution = provideNextExecution()
                val newIntervalTaskData = IntervalTaskData(
                    name = name,
                    nextExecution = nextLastExecution,
                )
                db.intervaltaskdata.updateOne(
                    IntervalTaskData::name eq name,
                    newIntervalTaskData,
                    upsert()
                )
                if (nextLastExecution > db.intervaltaskdata.get(name)!!.notAfter) break
                delay(nextLastExecution - Clock.System.now())
                consumer()
            }
        }
    }

    companion object : CoroutineScope {
        private val intervalTasks = mutableMapOf<IntervalTaskKey, IntervalTask>()
        suspend fun restartTask(name: IntervalTaskKey) {
            db.intervaltaskdata.deleteOne(IntervalTaskData::name eq name)
            intervalTasks[name]?.job?.cancel()
            intervalTasks[name]?.start()
        }
        fun setupIntervalTasks() {
            IntervalTask(YTSubscriptions, 4.days) {
                setupYTSuscribtions()
            }
            IntervalTask(Snips, {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, Random.nextInt(10..21))
                cal.set(Calendar.MINUTE, Random.nextInt(0..59))
                cal.set(Calendar.SECOND, Random.nextInt(0..59))
                Instant.fromEpochMilliseconds(cal.timeInMillis)
            }) {
                // jda will most likely not be ready here
                jda.awaitReady().getTextChannelById(1266027507019153473)!!
                    .sendMessage(possibleMessages.random()).queue()
            }
        }

        private val possibleMessages = setOf(
            "Hello! Vergangenheits-Lorelay hier! Enttäusch mich bitte nicht!",
            "Hallo! Ich bin Lorelay aus der Vergangenheit! Ich hoffe, du hast heute schon gelacht!",
            "Hey :) Vergangenheits-Lorelay hier! Ich weiß, wir schaffen das!",
            "Bonjour! Du weißt ja, wer hier ist! Auch 200 Wörter, machen den Unterschied!",
            "Du weißt Bescheid \uD83D\uDC40 \uD83D\uDC40 \uD83D\uDC40 ",
            "Vergangenheits-Lorelay glaubt, dass wir entweder \n" +
                    "a) eine Geschichte fertig kriegen\n" +
                    "b) zwei Geschichten sehr viel weiter kriegen\n" +
                    "Glaubst du auch noch daran?",
            "Der Literaturnobelpreis holt sich nicht von alleine! \uD83E\uDEE1 ",
            "Vergangenheits-Lorelay sagt: \"Schreib was!\"",
            "Wir beide wissen, dass sowohl dein Geist als auch dein Vokabular dadurch gut ausgelastet werden..."
        )

        override val coroutineContext = createCoroutineContext("IntervalTask")
    }
}

enum class IntervalTaskKey {
    YTSubscriptions,
    Snips
}
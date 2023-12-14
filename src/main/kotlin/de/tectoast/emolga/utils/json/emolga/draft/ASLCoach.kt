package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.coord
import de.tectoast.emolga.commands.defaultTimeFormat
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.repeat.RepeatTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA
import java.time.Duration
import java.util.Calendar.*

@Serializable
@SerialName("ASLCoach")
class ASLCoach : League() {
    val level by lazy { leaguename.last().digitToInt() }
    override val dataSheet = "Data$level"
    override val teamsize = 12
    override val pickBuffer = 5
    override val gamedays = 8

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(null) {
            b.addSingle(
                coord("Spielplan", gdi.x(5, 2), index.y(6, 5 + level)),
                defaultGameplanString
            )
        }
        //sorterData = SorterData(listOf("Tabellen!B5:J10", "Tabellen!B13:J18"), false, null, 2, 8, 6)
    }

    override fun provideReplayChannel(jda: JDA) = jda.getTextChannelById(1170477105339973824)
    override fun provideResultChannel(jda: JDA) = jda.getTextChannelById(1170477327839412365)


    @Transient
    override val timer = DraftTimer(
        TimerInfo(delaysAfterSkips = mapOf(0 to 120, 1 to 60, 2 to 30))
            .add(10, 22, SATURDAY, SUNDAY)
            .add(14, 22, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    )

    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            "Draftreihenfolge ${
                when (level) {
                    0 -> "Coaches"
                    else -> "Stufe $level"
                }
            }!${data.roundIndex.x(2, 2)}${data.indexInRound + 3}", data.pokemon
        )
    }

    companion object {
        fun setupRepeatTasks() {
            RepeatTask(
                defaultTimeFormat.parse("04.12.2023 00:00").toInstant(),
                5, Duration.ofDays(7)
            ) {
                val msg = "**------------- Spieltag $it -------------**"
                jda.getTextChannelById(1170477105339973824)!!.sendMessage(msg).queue()
                jda.getTextChannelById(1170477327839412365)!!.sendMessage(msg).queue()
            }
        }
    }
}


// Dasor steht hier, weil er das so wollte

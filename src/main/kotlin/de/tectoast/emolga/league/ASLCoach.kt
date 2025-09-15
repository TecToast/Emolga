package de.tectoast.emolga.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA
import kotlin.time.Duration.Companion.days

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
        newSystem(sorterData = null) {
            b.addSingle(
                coord("Spielplan", gdi.x(5, 2), index.y(7, 6 + level)), defaultGameplanString
            )
        }
    }

    override fun provideReplayChannel(jda: JDA) = jda.getTextChannelById(1283883488763707447)
    override fun provideResultChannel(jda: JDA) = jda.getTextChannelById(1283883574835154955)

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


    override fun setupRepeatTasks() {
        if (!leaguename.endsWith("1")) return
        RepeatTask(
            "ASL", RepeatTaskType.Other("Announce"), "16.12.2024 00:00", 8, 14.days, printDelays = true
        ) {
            val msg = "**------------- ${
                when (it) {
                    6 -> "Viertelfinale"
                    7 -> "Halbfinale"
                    8 -> "Finale"
                    else -> "Spieltag $it"
                }
            } -------------**"
            provideReplayChannel(jda)!!.sendMessage(msg).queue()
            provideResultChannel(jda)!!.sendMessage(msg).queue()
        }
    }

}
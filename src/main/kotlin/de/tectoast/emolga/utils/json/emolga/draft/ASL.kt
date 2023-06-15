package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command.Companion.getAsXCoord
import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.defaultTimeFormat
import de.tectoast.emolga.commands.draft.AddToTierlistData
import de.tectoast.emolga.commands.toDocRange
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.toastilities.repeat.RepeatTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA
import java.time.Duration
import java.util.Calendar.*

@Serializable
@SerialName("ASL")
class ASL(
    val replayChannel: Long,
    val resultChannel: Long
) : League() {
    override val dataSheet = "Data"
    override val pickBuffer = 18
    override val teamsize = 12
    override val gamedays = 10

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                formulaRange = "Tabelle!C4:J11".toDocRange(),
                directCompare = true,
                newMethod = true,
                cols = listOf(7, -1, 6, 4)
            ), resultCreator = {
                b.addSingle(
                    if (gdi in 2..7) gdi.minus(2).coordXMod("Spielplan", 3, 4, 3, 6, 10 + index)
                    else "Spielplan!" + getAsXCoord((gdi % 2) * 4 + 5) + ((gdi / 6) * 18 + 4 + index),
                    defaultGameplanString
                )
            })
        cancelIf = { _, gd -> gd == 10 }
    }

    @Transient
    override val additionalSet = AdditionalSet("N", "X", "Y")

    override fun provideReplayChannel(jda: JDA) = jda.getTextChannelById(replayChannel)
    override fun provideResultChannel(jda: JDA) = jda.getTextChannelById(resultChannel)

    override fun AddToTierlistData.addMonToTierlist() {
        builder().addRow("Data!K${index + 600}", listOf(mon, pkmn.speed, tier, englishTLName)).execute()
    }


    @Transient
    override val timer = DraftTimer(
        TimerInfo().add(10, 22, SATURDAY, SUNDAY).add(14, 22, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
        120
    )


    override val timerSkipMode = TimerSkipMode.LAST_ROUND

    override fun isFinishedForbidden() = false

    override fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.roundIndex.coordXMod("Draft", 6, 4, 3, 10, 4 + data.indexInRound), data.pokemon
        )
    }

    override fun RequestBuilder.switchDoc(data: SwitchData) {
        newSystemSwitchDoc(data)
        addRow("Zwischendraft!${data.roundIndex.x(4, 3)}${data.indexInRound + 4}", listOf(data.oldmon, data.pokemon))
    }

    companion object {
        fun setupRepeatTasks() {
            RepeatTask(
                defaultTimeFormat.parse("05.06.2023 00:00").toInstant(),
                7, Duration.ofDays(7)
            ) {
                val jda = EmolgaMain.emolgajda
                val msg = "**------------- Spieltag $it -------------**"
                for (i in 1..5) {
                    with(Emolga.get.drafts["ASLS12L$i"]!! as ASL) {
                        jda.getTextChannelById(replayChannel)!!.sendMessage(msg).queue()
                        jda.getTextChannelById(resultChannel)!!.sendMessage(msg).queue()
                    }
                }
            }
        }
    }

}

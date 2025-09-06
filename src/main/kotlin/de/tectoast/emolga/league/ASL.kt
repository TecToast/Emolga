package de.tectoast.emolga.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA
import kotlin.time.Duration.Companion.days

@Serializable
@SerialName("ASL")
class ASL(
    val replayChannel: Long
) : League() {
    override val dataSheet = "Data"
    override val pickBuffer = 18
    override val teamsize = 12
    override val gamedays = 10

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(SorterData(
            formulaRange = listOf(
                "Tabelle!C16:J19",
                "Tabelle!C24:J27",
            ), newMethod = true, cols = listOf(7, -1, 6, 4)
        ), resultCreator = {
            b.addSingle(
                if (gdi in 2..7) gdi.minus(2).coordXMod("Spielplan", 3, 4, 3, 6, 10 + index)
                else "Spielplan!" + getAsXCoord((gdi % 2) * 4 + 5) + ((gdi / 6) * 18 + 4 + index),
                defaultGameplanString
            )
        })
        //cancelIf = { _, gd -> gd == 10 }
    }

    override fun provideReplayChannel(jda: JDA) = jda.getTextChannelById(replayChannel)
    override fun provideResultChannel(jda: JDA) = jda.getTextChannelById(resultChannel!!)

    override suspend fun AddToTierlistData.addMonToTierlist() {
        val poke = pkmn.await()
        builder().addRow(
            "Data!B${index() + 600}", listOf(mon, tier, poke.getGen5SpriteFormula(), poke.speed, englishTLName.await())
        ).execute()
    }


    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.roundIndex.coordXMod("Draft", 6, 4, 3, 10, 4 + data.indexInRound), data.pokemon
        )
    }

    override suspend fun RequestBuilder.switchDoc(data: SwitchData) {
        newSystemSwitchDoc(data)
        addRow(
            "Zwischendraft!${data.roundIndex.x(4, 3)}${data.indexInRound + 4}",
            listOf(data.oldmon.tlName, data.pokemon)
        )
    }


    override fun setupRepeatTasks() {
        RepeatTask(
            "ASL", RepeatTaskType.Other("Announce"), "27.05.2024 00:00", 7, 7.days
        ) {
            executeGamedaySending(it)
        }
    }

    fun executeGamedaySending(gameday: Int) {
        val msg = "**------------- Spieltag $gameday -------------**"
        jda.getTextChannelById(replayChannel)!!.sendMessage(msg).queue()
        jda.getTextChannelById(resultChannel!!)!!.sendMessage(msg).queue()
    }


}

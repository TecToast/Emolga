package de.tectoast.emolga.league

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.coordXMod
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.JDA
import kotlin.time.Duration.Companion.days

@Serializable
@SerialName("ASLO")
class ASLO(
    val replayChannel: Long, val resultChannel: Long
) : League() {

    val conf = leaguename.last()
    val confidx = conf - 'A'

    override val dataSheet = "Data$confidx"
    override val pickBuffer = 18
    override val teamsize = 12
    override val gamedays = 11

    /*@Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                formulaRange = listOf(
                    "Tabelle!C4:J19",
                ), newMethod = true, cols = listOf(7, 6, 4)
            ), memberMod = 8, dataSheetProvider = { "Data${it / 8}" }, resultCreator = {
                b.addSingle(
                    if (gdi in 0..1) gdi.coordXMod("Spielplan", 2, 4, 5, 0, 4 + index)
                    else gdi.minus(2).coordXMod("Spielplan", 3, 4, 3, 10, 14 + index),
                    defaultGameplanString
                )
            })
    }*/

    override fun provideReplayChannel(jda: JDA) = jda.getTextChannelById(replayChannel)
    override fun provideResultChannel(jda: JDA) = jda.getTextChannelById(resultChannel)

    override suspend fun AddToTierlistData.addMonToTierlist() {
        val poke = pkmn.await()
        builder().addRow(
            "Data!B${index() + 600}", listOf(mon, tier, poke.getGen5Sprite(), poke.speed, englishTLName.await())
        ).execute()
    }


    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.roundIndex.coordXMod("Draft $conf", 6, 4, 3, 10, 4 + data.indexInRound), data.pokemon
        )
    }
    override suspend fun RequestBuilder.switchDoc(data: SwitchData) {
        newSystemSwitchDoc(data)
        addRow(
            data.roundIndex.coordXMod("PlayOffs-Draft $conf", 3, 4, 3, 5, data.indexInRound + 4),
            listOf(data.pokemon, data.oldmon.tlName)
        )
    }

    override fun setupRepeatTasks() {
        if (confidx == 0) {
            RepeatTask(
                "ASL", RepeatTaskType.Other("Announce"), "26.05.2025 00:00", 8, 7.days
            ) {
                executeGamedaySending(it)
            }
        }
    }

    fun executeGamedaySending(gameday: Int) {
        val msg = "**------------- Spieltag $gameday -------------**"
        jda.getTextChannelById(replayChannel)!!.sendMessage(msg).queue()
        jda.getTextChannelById(resultChannel)!!.sendMessage(msg).queue()
    }


}

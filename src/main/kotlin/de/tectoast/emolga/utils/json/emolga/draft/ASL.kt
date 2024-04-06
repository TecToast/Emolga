package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.features.draft.AddToTierlistData
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.JDA

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
                formulaRange = listOf(
                    "Tabelle!C4:J11",
                ),
                newMethod = true,
                cols = listOf(7, -1, 6, 4)
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
    override fun provideResultChannel(jda: JDA) = jda.getTextChannelById(resultChannel)

    override suspend fun AddToTierlistData.addMonToTierlist() {
        val poke = pkmn.await()
        builder().addRow(
            "Data!B${index() + 600}",
            listOf(mon, tier, poke.getGen5Sprite(), poke.speed, englishTLName.await())
        ).execute()
    }


//    @Transient
//    override var timer: DraftTimer? = SimpleTimer(
//        TimerInfo(delaysAfterSkips = mapOf(0 to 120, 1 to 60, 2 to 30)).add(10, 22, SATURDAY, SUNDAY)
//            .add(14, 22, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
//    )


    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.roundIndex.coordXMod("Draft", 6, 4, 3, 10, 4 + data.indexInRound), data.pokemon
        )
    }

    override suspend fun RequestBuilder.switchDoc(data: SwitchData) {
        newSystemSwitchDoc(data)
        addRow("Zwischendraft!${data.roundIndex.x(4, 3)}${data.indexInRound + 4}", listOf(data.oldmon, data.pokemon))
    }

}

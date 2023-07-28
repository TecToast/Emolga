package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.toCoord
import de.tectoast.emolga.utils.records.x
import de.tectoast.emolga.utils.records.y
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ADK")
class ADK : League() {
    override val teamsize = 11
    override val pickBuffer = 9
    override val timerSkipMode = TimerSkipMode.NEXT_PICK
    private val cid by lazy { leaguename.last().digitToInt() - 1 }
    override val dataSheet by lazy { "Data${cid + 1}" }

    @Transient
    override val timer = DraftTimer(TimerInfo(10, 22), 120)

    @Transient
    override val docEntry = DocEntry.create(this) {
        val startCoord = "Tabelle und Killliste" x "D" y 6
        newSystem(
            sorterData = SorterData(
                startCoord.plusX(cid.y('M' - 'C', 0)).spread(5, 7).toDocRange(),
                newMethod = true,
                cols = listOf(2, 3, 5)
            )
        ) {
            b.addSingle(
                gdi.coordXMod("Spielplan", 2, 'G' - 'C', cid.y('M' - 'C', 4), 21 - 13, 13), defaultGameplanString
            )
        }
    }


    private fun getFirstMonCoordByPlayerIndex(it: Int): String {
        return if (it in 0..1) "Kaderübersicht!${it.x('N' - 'D', cid.y('U' - 'D', 4))}18"
        else it.minus(2).coordXMod("Kaderübersicht", 3, 'I' - 'D', cid.y('U' - 'D', 4), 69 - 43, 44)
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            getFirstMonCoordByPlayerIndex(data.memIndex).toCoord().plusY(data.changedOnTeamsiteIndex).toString(),
            data.pokemon
        )
        addStrikethroughChange(
            1381578463,
            "${(data.roundIndex + 3).xc()}${cid.y(19 - 6, 6 + data.indexInRound)}",
            strikethrough = true
        )
    }

}

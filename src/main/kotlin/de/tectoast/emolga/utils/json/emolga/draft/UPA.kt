package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("UPA")
class UPA : League() {
    override val teamsize = 12

    @Transient
    override var timer: DraftTimer? = SimpleTimer(TimerInfo(10, 22, delayInMins = 120))
    override val afterTimerSkipMode = AFTER_DRAFT_ORDERED
    override val duringTimerSkipMode = NEXT_PICK

    val division by lazy { leaguename.last().digitToInt() }


    override val dataSheet: String
        get() = "Data$division"


    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.idx.coordXMod(
                "Kader", 2, 5, division.y('P' - 'C', 4), 34, 25 + data.changedOnTeamsiteIndex
            ), data.pokemon
        )
        addStrikethroughChange(
            340699480, "${(data.roundIndex + 3).xc()}${division.y(21 - 4, 6 + data.indexInRound)}", strikethrough = true
        )
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                listOf("Tabelle!D6:K13", "Tabelle!D18:K25"), newMethod = true, cols = listOf(3, 7, 5)
            )
        ) {
            b.addSingle(
                if (gdi == 6) "Spielplan!${division.x('N' - 'C', 6)}${35 + index}"
                else gdi.coordYMod("Spielplan", 3, 4, division.y('N' - 'C', 4), 9, 8 + index), defaultGameplanString
            )
        }
    }
}


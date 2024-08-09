package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("MDL")
class MDL : League() {
    override val teamsize = 12
    override val pickBuffer = 5

    @Transient
    override var timer: DraftTimer? = SimpleTimer(TimerInfo(9, 22))
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override val duringTimerSkipMode = NEXT_PICK

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.idx.coordXMod(
                "Kaderübersicht",
                4,
                6,
                4,
                17,
                6 + data.changedOnTeamsiteIndex
            ), data.pokemon
        )
        addStrikethroughChange(
            1846387109,
            "${(data.roundIndex + 3).xc()}${data.indexInRound + 5}",
            strikethrough = true
        )
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                listOf("Tabelle!C3:I10"),
                newMethod = true,
                cols = listOf(6, 5)
            )
        ) {
            b.addRow(
                gdi.CoordXMod("Spielplan", 3, 8, 3, 8, 5),
                listOf("$numberOne", "", """=HYPERLINK("$url"; ":")""", "$numberTwo")
            )
        }
    }

}

package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ADK")
class ADK : League() {
    override val teamsize = 11
    override val pickBuffer = 9
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override val duringTimerSkipMode = NEXT_PICK
    private val cid by lazy { leaguename.last().digitToInt() - 1 }
    override val dataSheet by lazy { "Data${cid + 1}" }

    @Transient
    override var timer: DraftTimer? = SimpleTimer(TimerInfo(10, 22, delayInMins = 120))

//    @Transient
//    override val docEntry = DocEntry.create(this) {
//        val startCoord = "Tabelle und Killliste" x "D" y 6
//        newSystem(
//            sorterData = SorterData(
//                startCoord.plusX(cid.y('M' - 'C', 0)).spreadBy(5, 5).toDocRange(),
//                newMethod = true,
//                cols = listOf(2, 3, 5)
//            )
//        ) {
//            b.addSingle(
//                gdi.coordXMod("Spielplan", 2, 'G' - 'C', cid.y('M' - 'C', 4), 21 - 13, 13 + index),
//                defaultGameplanString
//            )
//        }
//    }


    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.memIndex.coordXMod(
                "Kaderübersicht",
                3,
                'I' - 'C',
                cid.y('W' - 'C', 4),
                69 - 43,
                44 + data.changedOnTeamsiteIndex
            ),
            data.pokemon
        )
        addStrikethroughChange(
            1381578463,
            "${(data.roundIndex + 3).xc()}${cid.y(19 - 6, 6 + data.indexInRound)}",
            strikethrough = true
        )
    }

}

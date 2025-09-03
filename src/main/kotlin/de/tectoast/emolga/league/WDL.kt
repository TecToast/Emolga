package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("WDL")
class WDL : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = NEXT_PICK

    @Transient
    override val docEntry = DocEntry.create(this) {
        resultCreator = {
            val isDoubles = "Doubles" in leaguename
            b.addRow(
                Coord("Spielplan_S+D", if (isDoubles) "I" else "C", gdi.y(7, 6 + index)),
                listOf(numberOne, numberTwo)
            )
        }
        killProcessor = BasicStatProcessor {
            val isDoubles = "Doubles" in leaguename
            Coord("Kills-${if (isDoubles) "D" else "S"}-Einzeln", plindex.x(3, 3), gdi.y(14, 23 + monindex))
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val isDoubles = "Doubles" in leaguename
        val suffix = if (isDoubles) "D" else "S"
        addSingle(data.idx.CoordXMod("Kader-$suffix", 5, 2, 2, 18, 10 + data.changedOnTeamsiteIndex), data.pokemon)
        addStrikethroughChange(
            if (isDoubles) 177800946 else 472215678,
            data.roundIndex.y(2, 3),
            data.indexInRound + 7,
            true
        )
    }
}

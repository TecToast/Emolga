package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.*
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
        val isDoubles = "Doubles" in leaguename
        resultCreator = {
            b.addRow(
                Coord("Spielplan_S+D", if (isDoubles) "I" else "C", gdi.y(7, 6 + index)), listOf(numberOne, numberTwo)
            )
        }
        killProcessor = Bo3BasicStatProcessor {
            Coord("Kills-${if (isDoubles) "D" else "S"}-Einzeln", plindex.x(3, 3), gdi.y(14, 23 + monindex))
        }
        sorterData = defaultSorter(
            if (isDoubles) "Tabelle-S-D!A19:D28" else "Tabelle-S-D!A5:D14", indexer = {
                val coord = it.toCoord()
                (coord.x - 2) / 2 + (coord.y - 3) / 18 * 5
            }, if (isDoubles) listOf(
                TableCompareOption.WINS,
                TableCompareOption.WINS - TableCompareOption.LOSSES,
                TableCompareOption.WINS / TableCompareOption.LOSSES,
                TableCompareOption.KILLS,
                DirectCompareSortOption()
            ) else listOf(
                TableCompareOption.POINTS, TableCompareOption.DIFF, TableCompareOption.KILLS, DirectCompareSortOption()
            )
        )
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val isDoubles = "Doubles" in leaguename
        val suffix = if (isDoubles) "D" else "S"
        addSingle(data.idx.CoordXMod("Kader-$suffix", 5, 2, 2, 18, 10 + data.changedOnTeamsiteIndex), data.pokemon)
        addStrikethroughChange(
            if (isDoubles) 177800946 else 472215678, data.roundIndex.y(2, 3), data.indexInRound + 7, true
        )
    }
}

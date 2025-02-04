package de.tectoast.emolga.league

import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.ResultStatProcessor
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.y
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("RVL")
class RVL : League() {
    override val teamsize = 0

    @Transient
    override val docEntry = DocEntry.create(this) {
        this.winProcessor = ResultStatProcessor { Coord("Data", gameday + 2, plindex.y(3, 2 + winCountSoFar)) }
        this.looseProcessor = ResultStatProcessor { Coord("Data", gameday + 10, plindex.y(3, 2 + looseCountSoFar)) }
        resultCreator = {
            b.addSingle(gdi.CoordXMod("Spielplan", 3, 4, 3, 9, 3 + index), "$numberOne:$numberTwo")
        }
    }
}

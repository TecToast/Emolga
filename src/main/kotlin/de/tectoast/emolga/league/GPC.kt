package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("GPC")
class GPC(val teams: List<String>) : League() {
    override val teamsize = 11

    @Transient
    override val docEntry = DocEntry.create(this) {
        +StatProcessor {
            gdi.CoordXMod(teams[memIdx], 3, 4, 17, 9, 4 + monIterationIndex()) to DataTypeForMon.MONNAME
        }
        +StatProcessor {
            gdi.CoordXMod(teams[memIdx], 3, 4, 18, 9, 4 + monIterationIndex()) to DataTypeForMon.KILLS
        }
        +StatProcessor {
            gdi.CoordXMod(teams[memIdx], 3, 4, 19, 9, 4 + monIterationIndex()) to DataTypeForMon.DEATHS
        }
        resultCreator = {
            b.addSingle(gdi.coordXMod("Gruppen-Spielplan", 3, 5, 3, 8, 4 + index), defaultGameplanStringWithoutUrl)
            idxs.forEachIndexed { index, idx ->
                val baseString = "$higherNumber:$lowerNumber"
                b.addSingle(
                    gdi.CoordXMod(teams[idx], 3, 4, 19, 9, 2),
                    if (idx == fullGameData.winnerIdx) "S" else "N"
                )
            }
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val range = Coord("Kader", data.idx.x(3, 4), data.getTierInsertIndex() + 4)
        addSingle(range, data.pokemon)
        if (data.tera) {
            addSingle(range.plusX(1), "T")
        }
        addStrikethroughChange(2128049760, data.roundIndex + 2, data.indexInRound + 3, true)
    }
}

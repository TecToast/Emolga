package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("DL2")
class DL2 : League() {
    override val teamsize = 11
    override val duringTimerSkipMode = NEXT_PICK

    @Transient
    override val docEntry = DocEntry.create(this) {
        +StatProcessor {
            Coord("Killstats", gdi.y(3, 15), memIdx.y(21, 4 + monIndex())) to DataTypeForMon.KILLS
        }
        +StatProcessor {
            Coord("Killstats", gdi.y(3, 16), memIdx.y(21, 4 + monIndex())) to DataTypeForMon.DEATHS
        }
        resultCreator = {
            val baseCoord = if (gdi == 6) Coord("Standings & Schedule", "Z", 20) else gdi.CoordXMod(
                "Standings & Schedule", 3, 'X' - 'N', 16, 7, 6
            )
            b.addRow(baseCoord.plusY(index), listOf(numberOne, numberTwo))
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.idx.CoordXMod("rosterdata", 5, 7, 5, 34, data.picks.size + 2), data.pokemon)
    }
}

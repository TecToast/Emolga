package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("PPL")
class PPL(val sheetNames: List<String> = emptyList()) : League() {

    @Transient
    override val docEntry = DocEntry.create(this) {
        +StatProcessor {
            gdi.CoordXMod(sheetNames[this.memIdx], 3, 5, 2, 9, 36 + monIterationIndex()) to DataTypeForMon.MONNAME
        }
        +StatProcessor {
            gdi.CoordXMod(sheetNames[this.memIdx], 3, 5, 4, 9, 36 + monIterationIndex()) to DataTypeForMon.KILLS
        }
        +StatProcessor {
            gdi.CoordXMod(sheetNames[this.memIdx], 3, 5, 5, 9, 36 + monIterationIndex()) to DataTypeForMon.DEATHS
        }
        +StatProcessor {
            Coord(sheetNames[this.memIdx], "D", 18 + gdi) to OtherMonDataProvider.WinLossLiteral("S", "N")
        }
        +StatProcessor {
            Coord(sheetNames[this.memIdx], "E", 18 + gdi) to DataTypeForMon.DIFF
        }
    }

    override val teamsize = 12

    override val duringTimerSkipMode = ALWAYS

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val isMega = data.pokemonofficial.contains("-Mega")
        val offset = when {
            isMega -> 7
            data.freePick -> 7 + data.picks.count { it.free && !it.quit && !it.name.contains("-Mega") }
            else -> data.getTierInsertIndex()
        }
        val y = offset + 5
        val monCoord = Coord("Kader", data.idx.x(2, 2), y)
        addSingle(monCoord, data.pokemon)
        if (data.freePick || isMega) addSingle(monCoord.plusX(1), data.points ?: 0)
    }
}
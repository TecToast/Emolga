package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("WPP")
class WPP : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = NEXT_PICK

    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrderFromTierlist()
        +StatProcessor {
            memIdx.CoordXMod("Kader", 2, 'P' - 'B', 5 + gdi, 19, monIndex() + 9) to DataTypeForMon.KILLS
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.roundIndex.CoordXMod("Draft", 4, 5, 3, 11, 4 + data.indexInRound), data.pokemon)
        addSingle(Coord("Draft Hidden", "F", data.idx.y(11, data.picks.size)), data.tier)
    }
}

package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("GDL")
class GDL : League() {
    override val teamsize = 11
    override val duringTimerSkipMode = ALWAYS

    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrderFromTierlist()
        +StatProcessor {
            Coord("Kills Übersicht", 9 + gdi, memIdx.y(11, 3 + monIndex())) to DataTypeForMon.KILLS
        }
        resultCreator = {
            b.addRow(
                gdi.CoordXMod("Spielplan", 3, 8, 4, 7, 3 + index),
                buildDefaultSplitGameplanString("vs.")
            )
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.idx.CoordXMod("Kader DE", 5, 5, 3, 20, 10 + data.getTierInsertIndex()), data.pokemon)
    }
}
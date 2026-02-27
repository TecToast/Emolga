package de.tectoast.emolga.league

import de.tectoast.emolga.utils.DataTypeForMon
import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.StatProcessor
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.y
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ADL")
class ADL : League() {
    override val teamsize = 10

    @Transient
    override val docEntry = DocEntry.create(this) {
        +StatProcessor {
            Coord("Kills (type in)", 9 + gdi, memIdx.y(15, 3 + monIndex())) to DataTypeForMon.KILLS
        }
        resultCreator = {
            b.addRow(
                gdi.CoordXMod("Schedule", 3, 8, 4, 7, 3 + index),
                buildDefaultSplitGameplanString("vs.")
            )
        }
    }
}
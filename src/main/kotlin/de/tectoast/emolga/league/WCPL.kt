package de.tectoast.emolga.league

import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("WCPL")
class WCPL : League() {
    override val teamsize = 6

    @Transient
    override val docEntry = DocEntry.create(this) {
        resultCreator = {
            val base = gdi.CoordXMod("Spielplan", 2, 6, 4, 7, 6 + index)
            b.addSingle(base, numberOne)
            b.addSingle(base.plusX(2), numberTwo)
        }
    }
}
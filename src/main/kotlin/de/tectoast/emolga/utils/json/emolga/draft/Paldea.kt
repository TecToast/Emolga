package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.automation.structure.DocEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Paldea")
class Paldea : League() {

    override val teamsize = 12

    @Transient
    override val docEntry = DocEntry.create(this) {
        /*newSystem(
            SorterData(
                listOf("Tabellen!C3:J10".toDocRange(), "Tabellen!M3:T10".toDocRange()),
                directCompare = true,
                newMethod = true,
                cols = listOf(2, -1, 6)
            )
        ) {
            b.addSingle(
                "Spielplan!${((gdi % 4) * 4 + if (gdi < 4) 3 else 5).xc()}${
                    gdi.ydiv(
                        4,
                        9,
                        3 + index
                    ) + (if (dataSheet == "DataM") 4 else 0)
                }", defaultGameplanString
            )
        }*/
    }
}

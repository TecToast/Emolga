package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.toDocRange
import de.tectoast.emolga.commands.xc
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("WFS")
class WFS : League() {
    override val teamsize = 11
    override val pickBuffer = 10

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle("Kader!${data.memIndex.plus(3).xc()}${data.changedOnTeamsiteIndex + 6}", data.pokemon)
        val x = data.roundIndex.y(2, 2)
        addSingle("Draftreihenfolge!${x.xc()}${data.indexInRound + 20}", data.pokemon)
        addStrikethroughChange(1109858057, x, data.indexInRound + 4, true)
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                "Tabelle!C4:J15".toDocRange(), directCompare = true, newMethod = true, cols = listOf(2, 5, 3, -1)
            )
        ) {
            b.addSingle(gdi.coordXMod("Spielplan", 2, 'H' - 'D', 5, 17 - 9, 10 + index), defaultGameplanString)
        }
    }
}

package de.tectoast.emolga.league

import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.TableSortOption
import de.tectoast.emolga.utils.records.newSystemSorter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Wooloo")
class Wooloo : League() {
    override val teamsize = 12
    override val pickBuffer = 7

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(newSystemSorter("Tabelle!C6:J13", TableSortOption.fromCols(listOf(7, 6, 4)))) {
            b.addSingle(
                if (gdi == 6) Coord("Spielplan", "E", 22 + index) else gdi.CoordXMod(
                    "Spielplan", 2, 4, 3, 6, 4 + index
                ), defaultGameplanString
            )
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(data.roundIndex.CoordXMod("Draft", Int.MAX_VALUE, 2, 3, 0, 4 + data.indexInRound), data.pokemon)
    }
}
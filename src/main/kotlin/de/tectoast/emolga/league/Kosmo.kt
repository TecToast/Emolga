package de.tectoast.emolga.league

import de.tectoast.emolga.utils.BasicStatProcessor
import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.y
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Kosmo")
class Kosmo : League() {
    override val teamsize = 10

    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = BasicStatProcessor {
            Coord("Teamstatistik", gdi.y(3, 12), plindex.y(21, monindex + 4))
        }
        deathProcessor = BasicStatProcessor {
            Coord("Teamstatistik", gdi.y(3, 13), plindex.y(21, monindex + 4))
        }

        resultCreator = {
            val baseCoord = gdi.CoordXMod("Spielplan", 3, 'F' - 'B', 3, 11, 6 + index * 2)
            b.addSingle(
                baseCoord, defaultGameplanStringWithoutUrl
            )
            b.addSingle(baseCoord.plusY(1), url)
        }
    }
}

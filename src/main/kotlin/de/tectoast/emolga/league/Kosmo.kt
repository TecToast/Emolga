package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Kosmo")
class Kosmo(val nameTable: List<String>) : League() {
    override val teamsize = 10

    private fun StatProcessorData.create(offset: Int, provider: MonDataProvider) =
        Coord("Einzelstatistik ${nameTable[memIdx]}", gdi.x(1, 15), monIndex().y(4, 3 + offset)) to provider

    @Transient
    override val docEntry = DocEntry.create(this) {
        +StatProcessor {
            Coord("Teamstatistik", gdi.y(3, 12), memIdx.y(21, monIndex() + 4)) to DataTypeForMon.KILLS
        }
        +StatProcessor {
            Coord("Teamstatistik", gdi.y(3, 13), memIdx.y(21, monIndex() + 4)) to DataTypeForMon.DEATHS
        }
        +StatProcessor {
            create(0, DataTypeForMon.DAMAGE_DIRECT)
        }
        +StatProcessor {
            create(1, DataTypeForMon.DAMAGE_INDIRECT)
        }
        +StatProcessor {
            create(2, DataTypeForMon.DAMAGE_TAKEN)
        }
        +StatProcessor {
            create(3, DataTypeForMon.TURNS)
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

package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Nichts")
class Nichts(val sheetNames: List<String>) : League() {
    override val teamsize = 11
    override val pickBuffer = 6

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(Coord(sheetNames[data.idx], "E", 16 + run {
            when (data.tier) {
                "C" -> {
                    7 + data.picks.count { it.tier == "C" }
                }

                "D" -> {
                    10
                }

                else -> {
                    data.getTierInsertIndex()
                }
            }
        }), data.pokemon)
        addSingle(data.roundIndex.CoordXMod("Draft", 5, 3, 3, 10, 4 + data.indexInRound), data.pokemon)
    }
}
package de.tectoast.emolga.league

import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.*
import de.tectoast.emolga.utils.records.TableCompareOption.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Nichts")
class Nichts(val sheetNames: List<String>) : League() {
    override val teamsize = 11
    override val pickBuffer = 6

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            newSystemSorter(
                "Platzierungen!B2:H9", listOf(
                    WINS, WINS / LOSSES, DIFF, KILLS, DirectCompareSortOption()
                )
            ),
            bo3 = true
        ) {
            b.addRow(
                gdi.CoordYMod("Spielplan", 4, 6, 4, 6, 6 + index), listOf(numberOne, ":", numberTwo)
            )
            for (idx in idxs) {
                val sheet = sheetNames[idx]
                b.addSingle(Coord(sheet, 4 + gdi, 10), if (idx == winnerIndex) "W" else "L")
            }
        }
    }

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
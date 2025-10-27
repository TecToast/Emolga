package de.tectoast.emolga.league

import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.TableSortOption
import de.tectoast.emolga.utils.records.newSystemSorter
import de.tectoast.emolga.utils.x
import de.tectoast.emolga.utils.y
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("RIPL")
class RIPL : League() {
    override val teamsize = 12

    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override val duringTimerSkipMode = NEXT_PICK
    private val conf by lazy { leaguename.substringAfter("S4") }
    private val cid by lazy { if (conf == "Zeit") 0 else 1 }
    override val dataSheet = "Data$conf"

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addStrikethroughChange(
            703540571, data.roundIndex + 2, cid.y(19 - 7, 7 + data.indexInRound), strikethrough = true
        )
        val coord = data.idx.CoordXMod(
            "Kader $conf", 4, 2, 3, 19, data.changedOnTeamsiteIndex + 7
        )
        addSingle(coord.toString(), data.pokemon)
    }

    override suspend fun RequestBuilder.switchDoc(data: SwitchData) {
        newSystemSwitchDoc(data)
        addStrikethroughChange(
            770133001, data.roundIndex + 2, cid.y(19 - 7, 7 + data.indexInRound), strikethrough = true
        )
        val coord = data.idx.CoordXMod(
            "Kader $conf", 4, 2, 3, 19, data.changedOnTeamsiteIndex + 7
        )
        addSingle(coord.toString(), data.pokemon)
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            newSystemSorter(
                if (cid == 0) "Tabellen!C5:J12" else "Tabellen!C19:J26", TableSortOption.fromCols(listOf(1, 7, 5))
            )
        ) {
            b.addSingle("Spielplan!${cid.x('J' - 'B', 3)}${gdi.y(14 - 7, 8 + index)}", defaultGameplanStringWithoutUrl)
        }
    }
}

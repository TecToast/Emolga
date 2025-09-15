package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.TableSortOption
import de.tectoast.emolga.utils.records.defaultSorter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ADKL")
class ADKL : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = NEXT_PICK

    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrder = { list -> list.sortedBy { it.tier.indexedBy(tierlist.order) }.map { it.name } }
        killProcessor = BasicStatProcessor {
            plindex.CoordXMod("Kader", 2, 'P' - 'B', 5 + gdi, 22, monindex + 11)
        }
        deathProcessor = CombinedStatProcessor {
            plindex.CoordXMod("Kader", 2, 'P' - 'B', 5 + gdi, 22, 22)
        }
        sorterData = defaultSorter(
            "Tabelle!C5:H12",
            indexer = { it.substringAfter("C").substringBefore(":").toInt() },
            sortOptions = TableSortOption.fromCols(listOf(5, -1, 4))
        )
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addStrikethroughChange(57357925, data.round + 1, data.indexInRound + 5, true)
        addSingle(data.idx.CoordXMod("Kader", 2, 'P' - 'B', 3, 22, 11 + data.changedOnTeamsiteIndex), data.pokemon)
    }
}

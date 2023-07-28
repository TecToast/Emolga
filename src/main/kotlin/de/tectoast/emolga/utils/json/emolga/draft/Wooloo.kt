package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.toDocRange
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.CombinedStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.ResultStatProcessor
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Wooloo")
class Wooloo : League() {
    override val teamsize = 12

    val teams: Map<Int, Map<Long, List<DraftPokemon>>> = mutableMapOf()

    override fun providePicksForGameday(gameday: Int) = teams[gameday]!!

    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = CombinedStatProcessor {
            Coord("Data", gameday + 2, plindex.y(3, 2))
        }
        killProcessor = BasicStatProcessor {
            battleindex.CoordXMod("Spieltag $gameday", 2, 'I' - 'B', 4 + gameplanIndex, 16, 5 + monindex)
        }
        deathProcessor = CombinedStatProcessor {
            Coord("Data", gameday + 3 + gamedays, plindex.y(3, 2))
        }
        winProcessor = ResultStatProcessor {
            Coord("Data", gameday + 2, plindex.y(3, 3))
        }
        looseProcessor = ResultStatProcessor {
            Coord("Data", gameday + 3 + gamedays, plindex.y(3, 3))
        }
        rowNumToIndex = { it.minus(4).div(3) }
        sorterData =
            SorterData("Tabelle!C3:J10".toDocRange(), directCompare = true, newMethod = true, cols = listOf(7, -1, 6))
        resultCreator = {
            b.addSingle(index.coordXMod("Spieltag ${gdi + 1}", 2, 'I' - 'B', 2, 16, 2), defaultGameplanString)
        }
    }
}

package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.toDocRange
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.ResultStatProcessor
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Prisma")
class Prisma : League() {
    override val teamsize = 12

    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = BasicStatProcessor {
            Coord(
                "Data", gameday + 6, plindex * 11 + 2 + monindex
            )
        }
        deathProcessor = BasicStatProcessor {
            Coord(
                "Data", gameday + 14, plindex * 11 + 2 + monindex
            )
        }
        winProcessor = ResultStatProcessor {
            Coord(
                "Data", "W", plindex * 11 + 1 + gameday
            )
        }
        looseProcessor = ResultStatProcessor {
            Coord(
                "Data", "X", plindex * 11 + 1 + gameday
            )
        }
        resultCreator = {
            b.addSingle(
                "Spielplan!${Command.getAsXCoord((if (gdi == 6) 1 else gdi % 3) * 3 + 3)}${gdi / 3 * 5 + 4 + index}",
                defaultGameplanString
            )
        }
        setStatIfEmpty = false
        sorterData = SorterData(
            "Tabelle!B2:I9".toDocRange(),
            true,
            { it.substring("=Data!W".length).toInt() / 11 - 1 },
            cols = listOf(1, -1, 7)
        )
    }
    override val timer get() = error("not implemented")

}

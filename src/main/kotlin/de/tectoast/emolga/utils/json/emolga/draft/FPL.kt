package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordYMod
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.automation.structure.BasicResultCreator
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.ResultStatProcessor
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.StatLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("FPL")
class FPL(val level: Int) : League() {
    override val timer get() = error("not implemented")

    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation(
                "Data$level",
                gameday + 2,
                plindex.y(25, 3 + monindex)
            )
        }
        deathProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation(
                "Data$level",
                gameday + 14,
                plindex.y(25, 3 + monindex)
            )
        }
        winProcessor = ResultStatProcessor { plindex, gameday ->
            StatLocation(
                "Data$level",
                gameday + 2,
                plindex.y(25, 25)
            )
        }
        looseProcessor = ResultStatProcessor { plindex, gameday ->
            StatLocation(
                "Data$level",
                gameday + 14,
                plindex.y(25, 25)
            )
        }
        resultCreator = BasicResultCreator { b, gdi, index, numberOne, numberTwo, url ->
            b.addRow(
                gdi.coordYMod("Spielplan L$level Spoiler", 4, 6, 4, 6, 6 + index),
                listOf(numberOne, "=HYPERLINK(\"$url\"; \":\")", numberTwo)
            )
        }
        sorterData = SorterData("Tabelle L$level!B2:H9", false, null, 1, 4, 2)
        randomGamedayMapper = {
            if (level == 1) {
                when (it) {
                    1 -> 7
                    2 -> 2
                    3 -> 3
                    4 -> 1
                    5 -> 5
                    6 -> 6
                    7 -> 4
                    else -> error("invalid gameday")
                }
            } else {
                when (it) {
                    1 -> 7
                    2 -> 3
                    3 -> 5
                    4 -> 2
                    5 -> 4
                    6 -> 1
                    7 -> 6
                    else -> error("invalid gameday")
                }
            }
        }
    }
}
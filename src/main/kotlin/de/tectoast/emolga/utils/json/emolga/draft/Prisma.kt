package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.RequestBuilder
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
@SerialName("Prisma")
class Prisma : League() {

    @Transient
    override val docEntry = DocEntry.create {
        league = this@Prisma
        killProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 6,
                plindex * 11 + 2 + monindex
            )
        }
        deathProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 14,
                plindex * 11 + 2 + monindex
            )
        }
        winProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int ->
                StatLocation(
                    "Data",
                    "W",
                    plindex * 11 + 1 + gameday
                )
            }
        looseProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int ->
                StatLocation(
                    "Data",
                    "X",
                    plindex * 11 + 1 + gameday
                )
            }
        resultCreator =
            BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, numberOne: Int, numberTwo: Int, url: String? ->
                b.addSingle(
                    "Spielplan!${Command.getAsXCoord((if (gdi == 6) 1 else gdi % 3) * 3 + 3)}${gdi / 3 * 5 + 4 + index}",
                    "=HYPERLINK(\"$url\"; \"$numberOne:$numberTwo\")"
                )
            }
        setStatIfEmpty = false
        sorterData = SorterData(
            "Tabelle!B2:I9",
            true, { s: String -> s.substring("=Data!W".length).toInt() / 11 - 1 }, 1, -1, 7
        )
    }
    override val timer get() = error("not implemented")

}
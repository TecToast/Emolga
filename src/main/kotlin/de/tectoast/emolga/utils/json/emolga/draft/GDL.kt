package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.BasicResultCreator
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.CombinedStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.records.StatLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
@SerialName("GDL")
class GDL : League() {
    @Transient
    override val docEntry = DocEntry.create {
        league = this@GDL
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 15 + 5 + monindex)
        }
        deathProcessor = CombinedStatProcessor { plindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 15 + 16)
        }
        resultCreator =
            BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, numberOne: Int, numberTwo: Int, url: String ->
                b.addRow(
                    "Spielplan!${Command.getAsXCoord(gdi % 3 * 6 + 3)}${gdi / 3 * 9 + 4 + index}",
                    listOf(numberOne, "=HYPERLINK(\"$url\"; \":\")", numberTwo)
                )
            }
    }

}
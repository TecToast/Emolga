package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.*
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.records.StatLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
@SerialName("GDL")
class GDL : League() {
    @Transient
    override val docEntry = DocEntry.create {
        leagueFunction = LeagueFunction { uid1: Long, uid2: Long ->
            val emolga = Emolga.get
            if (emolga.league("GDL1").table.containsAll(listOf(uid1, uid2))) {
                emolga.league("GDL1")
            } else emolga.league("GDL2")
        }
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
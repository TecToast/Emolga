package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.CombinedStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.records.StatLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("DoR")
class DoR : League() {
    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 17 + 6 + monindex)
        }
        deathProcessor = CombinedStatProcessor { plindex, gameday ->
            StatLocation("Kader", plindex % 2 * 14 + 5 + gameday, plindex / 2 * 17 + 18)
        }
        resultCreator = {
            b.addRow(
                "Spielplan!${Command.getAsXCoord(gdi / 5 * 6 + 3)}${gdi % 5 * 10 + 7 + index + (index / 2)}",
                listOf(numberOne, "=HYPERLINK(\"$url\"; \":\")", numberTwo)
            )
        }
        monsOrder = { l -> l.sortedWith(compareBy({ it.free }, { if (it.free) "" else it.tier })).map { it.name } }
    }

    @Transient
    override val timer = DraftTimer(TimerInfo(12, 22), 60)

    override fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.memIndex.coordXMod("Kader",
            2,
            14,
            4,
            17,
            if (data.freePick) data.picks.count { it.free } + 13 else data.changedOnTeamsiteIndex + 6),
            data.pokemon)
        addSingle(data.round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + data.indexInRound), data.pokemon)
        if (data.freePick) addSingle(
            data.round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 5, 13, 3 + data.indexInRound), "F"
        )
    }

    override val timerSkipMode = TimerSkipMode.NEXT_PICK
}

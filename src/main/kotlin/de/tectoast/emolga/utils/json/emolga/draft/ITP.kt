package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.*
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
@SerialName("ITP")
class ITP : League() {
    @Transient
    override val timer = DraftTimer(TimerInfo(9, 22), 120)

    val teraTypes: MutableMap<Long, String> = mutableMapOf()

    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation(
                "Kader",
                plindex.xmod(2, 'W' - 'F', 6 + gameday),
                plindex.ydiv(2, 21 - 2, 9 + monindex)
            )
        }
        deathProcessor =
            CombinedStatProcessor { plindex, gameday -> StatLocation("Hidden Tabelle", gameday + 5, plindex + 20) }
        resultCreator = {
            b.addRow(
                gdi.coordYMod("Spielplan", 3, 'H' - 'B', 3, 17 - 5, 6 + 2 * index), listOf(
                    numberOne, "=HYPERLINK(\"$url\"; \":\")", numberTwo
                )
            )
        }
        monsOrder = { l ->
            l.sortedWith(compareBy({ it.free }, { if (it.free) 0 else it.tier.indexedBy(tierlist.order) }))
                .map { it.name }
        }
    }

    override fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + data.indexInRound), data.pokemon)
        addSingle(data.memIndex.coordXMod("Kader",
            2,
            17,
            3,
            19,
            if (data.freePick) data.picks.count { it.free } + 15 else 9 + data.changedOnTeamsiteIndex), data.pokemon)
    }

    override fun beforePick() =
        if (isLastRound && current !in teraTypes) "Du musst noch deinen Tera-Typ picken!" else null

    override fun reset() {
        teraTypes.clear()
    }

    override val timerSkipMode = TimerSkipMode.NEXT_PICK

    fun typeDoc(type: String) {
        val b = builder()
        b.addSingle(round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + indexInRound(round)), type)
        b.addSingle(
            table.indexOf(current).coordXMod(
                "Kader",
                2,
                17,
                6,
                19,
                4
            ), "Tera-Typ: $type"
        )
        b.execute()
    }

}

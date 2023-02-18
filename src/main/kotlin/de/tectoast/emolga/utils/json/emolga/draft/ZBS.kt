package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.toDocRange
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ZBS")
class ZBS(private val conference: String) : League() {
    @Transient
    override val timer = DraftTimer(TimerInfo(10, 22), 120)

    override val timerSkipMode = TimerSkipMode.NEXT_PICK

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                formulaRange = "$conference-Tabelle!C4:J11".toDocRange(),
                directCompare = false,
                cols = listOf(2, 6, 4)
            )
        ) {}
    }

    override val pickBuffer = 12

    override fun pickDoc(data: PickData) {
        // =ZÄHLENWENN(INDIREKT("DataRotschopf!B500:B587"); B4)
        val b = builder()
        val mon = data.pokemon
        b.addSingle(data.roundIndex.coordXMod("$conference-Draftreihenfolge", 6, 2, 3, 10, 3 + data.indexInRound), mon)
        val monCoord = "Data$conference!B${data.memIndex.y(26, 3 + data.changedIndex)}"
        b.addSingle(monCoord, mon)
        b.addSingle("$conference-Conference!C${data.memIndex.y(14, getTierInsertIndex(data) + 4)}", mon)
        b.execute()
    }
}

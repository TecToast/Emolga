package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.DocEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ZBS")
class ZBS(private val conference: String) : League() {
    override val teamsize = 11

    @Transient
    override val timer = DraftTimer(TimerInfo(10, 22))

    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override val duringTimerSkipMode = NEXT_PICK
    override val pickBuffer = 12

    override val dataSheet
        get() = "Data$conference"

    @Transient
    override val docEntry = DocEntry.create(this) {
        /*newSystem(
            SorterData(
                formulaRange = "$conference-Tabelle!C4:J11".toDocRange(),
                directCompare = false,
                cols = listOf(2, 6, 4)
            )
        ) {}*/
    }


    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        // =ZÄHLENWENN(INDIREKT("DataRotschopf!B500:B587"); B4)
        val mon = data.pokemon
        addSingle(data.roundIndex.coordXMod("$conference-Draftreihenfolge", 6, 2, 3, 10, 3 + data.indexInRound), mon)
        val monCoord = "Data$conference!B${data.memIndex.y(26, 3 + data.changedIndex)}"
        addSingle(monCoord, mon)
        addSingle("$conference-Conference!C${data.memIndex.y(14, data.changedOnTeamsiteIndex + 4)}", mon)
    }
}

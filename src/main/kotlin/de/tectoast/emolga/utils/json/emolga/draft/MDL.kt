package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("MDL")
class MDL(val division: Int) : League() {
    override val teamsize = 11

    @Transient
    override val timer = DraftTimer(TimerInfo(9, 22), 120)
    override val timerSkipMode = TimerSkipMode.NEXT_PICK

    val jokers = mutableMapOf<Long, Int>()

    @Transient
    var currentMon: MDLPick? = null

    override fun reset() {
        jokers.clear()
        table.forEach { jokers[it] = 3 }
    }

    override val dataSheet: String
        get() = "Data$division"

    override fun isPicked(mon: String, tier: String?) = picks.values.flatten().any { p ->
        p.name.equals(
            mon,
            ignoreCase = true
        )
    } || Emolga.get.league("MDLL${2 - division}").picks.values.flatten()
        .any { p -> p.name.equals(mon, ignoreCase = true) }

    override fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(
            data.memIndex.coordXMod(
                "Kader",
                2,
                5,
                division.y('P' - 'C', 4),
                34,
                25 + data.changedOnTeamsiteIndex
            ), data.pokemon
        )
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(
            SorterData(
                listOf("Tabelle!D6:K13".toDocRange(), "Tabelle!D18:K25".toDocRange()),
                newMethod = true,
                cols = listOf(3, 7, 5)
            )
        ) {
            b.addSingle(
                if (gdi == 6) "Spielplan!${division.x('N' - 'C', 6)}${35 + index}"
                else gdi.coordYMod("Spielplan", 3, 4, division.y('N' - 'C', 4), 9, 8 + index), defaultGameplanString
            )
        }
    }

    override fun beforePick() = "Ne ne, der normale Pick-Command ist in der MDL keine Sache :)"
}

data class MDLPick(val official: String, val tlName: String, val tier: String, val type: String)
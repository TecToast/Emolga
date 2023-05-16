package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
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

    override fun beforePick() = "Ne ne, der normale Pick-Command ist in der MDL keine Sache :)"
}

data class MDLPick(val official: String, val tlName: String, val tier: String, val type: String)

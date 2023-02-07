package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.TimerInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ITP")
class ITP : League() {
    @Transient
    override val timer = DraftTimer(TimerInfo(9, 22), 120)

    val teraTypes: MutableMap<Long, String> = mutableMapOf()

    override fun pickDoc(data: PickData) {
        val b = builder()
        b.addSingle(data.round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + data.indexInRound), data.pokemon)
        b.addSingle(data.memIndex.coordXMod("Kader",
            2,
            17,
            3,
            19,
            if (data.freePick) data.picks.count { it.free } + 15 else 9 + getTierInsertIndex(data)), data.pokemon)
        b.execute()
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

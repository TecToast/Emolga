package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.draft.PickData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("DoR")
class DoR : League() {
    @Transient
    override val docEntry = null

    override fun afterPick() {
        TODO("not implemented")
    }

    override fun pickDoc(data: PickData) {
        val b = builder()
        b.addSingle(data.memIndex.coordXMod("Kader",
            2,
            14,
            4,
            17,
            if (data.freePick) data.picks.count { it.free } + 13 else getTierInsertIndex(data.picks, data.tier)),
            data.pokemon)
        b.addSingle(data.round.minus(1).coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + data.indexInRound), data.pokemon)
        b.execute()
    }
}
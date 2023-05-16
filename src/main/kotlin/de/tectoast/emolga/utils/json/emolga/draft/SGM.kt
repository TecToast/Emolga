package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.utils.RequestBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SGM")
class SGM : League() {
    override val teamsize = 9
    override val pickBuffer = 7

    override val timerSkipMode = TimerSkipMode.AFTER_DRAFT

    override fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(data.memIndex.coordXMod("Kader", 2, 'R' - 'B', 3, 19, 9 + data.changedOnTeamsiteIndex), data.pokemon)
        addSingle(data.roundIndex.coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + data.indexInRound), data.pokemon)
    }
}

package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.x
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("NDSML")
class NDSML : League() {
    override val teamsize = 11
    override val pickBuffer = 6

    @Transient
    override val timer = DraftTimer(TimerInfo(mapOf(0 to 180, 1 to 150, 2 to 120, 3 to 90, 4 to 60)).set(10, 22))

    override val duringTimerSkipMode = NEXT_PICK
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle("Data!AA${data.memIndex.y(newSystemGap, data.changedOnTeamsiteIndex + 3)}", data.pokemon)
        addSingle("Draftreihenfolge!${data.roundIndex.x(2, 2)}${data.indexInRound.y(2, 5)}", data.pokemon)
    }
}

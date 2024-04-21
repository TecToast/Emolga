package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("RRL")
class RRL : League() {
    override val teamsize = 11

    @Transient
    override var timer: DraftTimer? = SimpleTimer(TimerInfo(9, 22, delayInMins = 60))
    override val afterTimerSkipMode = AFTER_DRAFT_ORDERED
    override val duringTimerSkipMode = NEXT_PICK

    val division by lazy { leaguename.last().digitToInt() }


    override val dataSheet: String
        get() = "Data$division"


    override suspend fun RequestBuilder.pickDoc(data: PickData) {
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
        addStrikethroughChange(
            340699480,
            "${(data.roundIndex + 3).xc()}${division.y(21 - 4, 6 + data.indexInRound)}",
            strikethrough = true
        )
    }
}

package de.tectoast.emolga.league

import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("RPL")
class RPL : League() {

    override val teamsize = 11
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    override val duringTimerSkipMode = NEXT_PICK

    /*@Transient
    override var timer: DraftTimer? = SimpleTimer(
        TimerInfo(delaysAfterSkips = mapOf(0 to 4 * 60, 1 to 2 * 60, 2 to 60, 3 to 30, 4 to 15)).apply {
            set(10, 22)
            startPunishSkipsTime = 1734177600000
        }
    )*/
    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrder = { list -> list.sortedBy { it.tier.indexedBy(tierlist.order) }.map { it.name } }
        +StatProcessor {
            memIdx.CoordXMod("Kader", 2, 'R' - 'B', 5 + gdi, 19, monIndex() + 9) to DataTypeForMon.KILLS
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.roundIndex.CoordXMod("Draft", 6, 5, 3, 13, 4 + data.indexInRound), data.pokemon)
    }


}

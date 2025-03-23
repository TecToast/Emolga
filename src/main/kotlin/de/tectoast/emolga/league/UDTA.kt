package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.x
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("UDTA")
class UDTA : League() {
    override val teamsize = 12

    override val duringTimerSkipMode = NEXT_PICK

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val x = data.idx.x(2, 3)
        if (data.freePick) {
            addRow("Kader!$x${data.picks.count { it.free } + 13}",
                listOf(data.pokemon, tierlist.getPointsNeeded(data.tier)))
        } else {
            addSingle("Kader!$x${data.changedOnTeamsiteIndex + 6}", data.pokemon)
        }
        data.tera?.let {
            addRow("Kader!${x}5", listOf(data.pokemon, it))
        }
        addStrikethroughChange(
            1901315365,
            (data.roundIndex / 2) * 3 + 2 + (data.roundIndex % 2),
            data.indexInRound + 6,
            true
        )
    }

    override suspend fun isPicked(mon: String, tier: String?): Boolean {
        if (mon.startsWith("Ogerpon")) {
            return picks.values.flatten().any { it.name.startsWith("Ogerpon") }
        }
        return super.isPicked(mon, tier)
    }
}
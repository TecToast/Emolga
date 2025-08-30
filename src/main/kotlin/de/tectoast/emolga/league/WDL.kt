package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import de.tectoast.emolga.utils.y
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WDL")
class WDL : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = NEXT_PICK

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val isDoubles = "Doubles" in leaguename
        val suffix = if (isDoubles) "D" else "S"
        addSingle(data.idx.CoordXMod("Kader-$suffix", 5, 2, 2, 18, 10 + data.changedOnTeamsiteIndex), data.pokemon)
        addStrikethroughChange(
            if (isDoubles) 177800946 else 472215678,
            data.roundIndex.y(2, 3),
            data.indexInRound + 7,
            true
        )
    }
}

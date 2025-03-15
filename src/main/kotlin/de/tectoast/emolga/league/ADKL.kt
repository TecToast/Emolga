package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ADKL")
class ADKL : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = NEXT_PICK

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addStrikethroughChange(57357925, data.round + 1, data.indexInRound + 5, true)
        addSingle(data.idx.CoordXMod("Kader", 2, 'P' - 'B', 3, 22, 11 + data.changedOnTeamsiteIndex), data.pokemon)
    }
}
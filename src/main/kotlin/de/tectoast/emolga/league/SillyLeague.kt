package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Silly")
class SillyLeague : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = ALWAYS

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(
            data.idx.CoordXMod("Draft", 8, 'K' - 'G', 7, 15, 8 + run {
                if(data.tier == "C") {
                    if(data.picks.count { it.tier == "C" } > 1) 9 else 8
                } else if(data.tier == "D") {
                    if(data.picks.count { it.tier == "D" } > 1) 9 else 10
                } else {
                    data.getTierInsertIndex()
                }
            }),
            data.pokemon)
        // TODO: Support for C/D tiers
    }
}

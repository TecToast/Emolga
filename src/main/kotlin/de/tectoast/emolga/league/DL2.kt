package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("DL2")
class DL2 : League() {
    override val teamsize = 11

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.idx.CoordXMod("rosterdata", 5, 7, 5, 34, data.picks.size + 2), data.pokemon)
    }
}

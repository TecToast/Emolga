package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GDL")
class GDL : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = ALWAYS

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.idx.CoordXMod("Kader DE", 5, 5, 3, 20, 10 + data.getTierInsertIndex()), data.pokemon)
    }
}
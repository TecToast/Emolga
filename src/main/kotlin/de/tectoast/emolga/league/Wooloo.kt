package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Wooloo")
class Wooloo : League() {
    override val teamsize = 12
    override val pickBuffer = 7

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(data.roundIndex.CoordXMod("Draft", Int.MAX_VALUE, 2, 3, 0, 4 + data.indexInRound), data.pokemon)
    }
}
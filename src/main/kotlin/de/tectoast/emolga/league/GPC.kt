package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.x
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GPC")
class GPC : League() {
    override val teamsize = 11

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val range = Coord("Kader", data.idx.x(3, 4), data.getTierInsertIndex() + 4)
        addSingle(range, data.pokemon)
        if (data.tera) {
            addSingle(range.plusX(1), "T")
        }
        addStrikethroughChange(2128049760, data.roundIndex + 2, data.indexInRound + 3, true)
    }
}

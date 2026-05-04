package de.tectoast.emolga.league

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.x
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PPL")
class PPL : League() {
    override val teamsize = 12

    override val duringTimerSkipMode = ALWAYS

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val isMega = data.pokemonofficial.contains("-Mega")
        val offset = when {
            isMega -> 7
            data.freePick -> 7 + data.picks.count { it.free && !it.quit }
            else -> data.getTierInsertIndex()
        }
        val y = offset + 5
        val monCoord = Coord("Kader", data.idx.x(2, 2), y)
        addSingle(monCoord, data.pokemon)
        if (data.freePick || isMega) addSingle(monCoord.plusX(1), data.points ?: 0)
    }
}
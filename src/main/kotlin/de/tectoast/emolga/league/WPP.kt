package de.tectoast.emolga.league

import de.tectoast.emolga.utils.BasicStatProcessor
import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("WPP")
class WPP : League() {
    override val teamsize = 11

    override val duringTimerSkipMode = NEXT_PICK
    override val afterTimerSkipMode = AFTER_DRAFT_ORDERED

    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrder = { list -> list.sortedBy { it.tier.indexedBy(tierlist.order) }.map { it.name } }
        killProcessor = BasicStatProcessor {
            plindex.CoordXMod("Kader", 2, 'Q' - 'B', 6 + gdi, 19, monindex + 9)
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.roundIndex.CoordXMod("Draft", 4, 5, 3, 11, 4 + data.indexInRound), data.pokemon)
    }
}

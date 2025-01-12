package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.BasicStatProcessor
import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("EPP")
class EPP(val spoilerSid: String) : League() {
    override val teamsize = 11
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED

    /*@Transient
    override var timer: DraftTimer? = SimpleTimer(
        TimerInfo(delaysAfterSkips = mapOf(0 to 4 * 60, 1 to 2 * 60, 2 to 60, 3 to 30, 4 to 15)).apply {
            set(10, 22)
            startPunishSkipsTime = 1734177600000
        }
    )*/
    @Transient
    override val docEntry = DocEntry.create(this) {
        monsOrder = { list -> list.sortedBy { it.tier.indexedBy(tierlist.order) }.map { it.name } }
        spoilerDocSid = spoilerSid
        killProcessor = BasicStatProcessor {
            plindex.CoordXMod("Kader", 2, 'P' - 'B', 5 + gdi, 19, monindex + 9)
        }
    }

    override fun sendRound() {
        tc.sendMessage("## === Runde ${if (round == 1) "Bann" else round - 1} ===").queue()
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addSingle(data.roundIndex.minus(1).CoordXMod("Draft", 6, 5, 3, 11, 4 + data.indexInRound), data.pokemon)
    }

    override suspend fun RequestBuilder.banDoc(data: BanData) {
        addSingle(Coord("Draft", "AB", 15 + data.indexInRound), data.pokemon)
    }


    init {
        /*enableConfig(
            DraftBanConfig(
                banRounds = mapOf(
                    1 to BanRoundConfig.FixedTierSet(
                        mapOf("S" to 1, "A" to 1, "B" to 2, "C" to 2, "D" to 2)
                    )
                ),
                notBannable = setOf(
                    "Epitaff",
                    "Qurtel",
                    "Rotom-Wash",
                    "Suelord",
                    "Florges",
                    "Quaxo",
                    "Folipurba",
                    "Zapplarang",
                    "Sengo",
                    "Skorgla",
                    "Spinsidias"
                ),
                skipBehavior = BanSkipBehavior.RANDOM
            )
        )*/
    }
}
package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.CombinedStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.records.CoordXMod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("RIPL")
class RIPL : League() {
    override val teamsize = 12

    @Transient
    override val timer = DraftTimer(TimerInfo(8, 23), 10)
    override val timerSkipMode = TimerSkipMode.NEXT_PICK
    private val cid by lazy { leaguename.last().digitToInt() - 1 }
    private val conf by lazy { if (cid == 0) "Sun" else "Moon" }
    override fun checkUpdraft(specifiedTier: String, officialTier: String) = when {

        specifiedTier.startsWith("Mega") && officialTier != specifiedTier -> "Mega-Entwicklungen können nicht hochgedraftet werden!"
        officialTier.startsWith("Mega") && !specifiedTier.startsWith("Mega") -> "Mega-Entwicklungen müssen im Mega-Tier gepickt werden!"
        else -> null
    }.also { logger.info("specifiedTier: $specifiedTier, officialTier: $officialTier") }

    val dataSid = ""

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        addStrikethroughChange(
            703540571,
            data.roundIndex + 2,
            cid.y(21 - 6, 7 + data.indexInRound),
            strikethrough = true
        )
        val isMega = data.pokemonofficial.isMega
        val coord = data.memIndex.CoordXMod("Kader $conf",
            5,
            2,
            3,
            20,
            if (isMega) 14 else if (data.freePick) data.picks.count { it.free } + 14 else data.changedOnTeamsiteIndex + 7)
        addSingle(coord.toString(), data.pokemon)
        if (data.freePick || isMega) {
            addSingle(coord.plusX(1).toString(), tierlist.getPointsNeeded(data.tier).let { if (isMega) -it else it })
        }
        RequestBuilder(dataSid).addSingle(
            data.memIndex.coordXMod(conf, 2, 28, 1, 29, data.picks.size + 4),
            data.pokemon
        ).execute()
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        customDataSid = dataSid
        killProcessor = BasicStatProcessor {
            plindex.CoordXMod(conf, 2, 28, gdi + 2, 29, monindex + 5)
        }
        deathProcessor = CombinedStatProcessor {
            plindex.CoordXMod(conf, 2, 28, gdi + 2, 29, 29)
        }
        resultCreator = {
            b.addSingle("Spielplan!${cid.x('L' - 'B', 3)}${gdi.y(13 - 5, 6 + index)}", defaultGameplanString)
        }
    }
}

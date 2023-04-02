package de.tectoast.emolga.utils.json.emolga.draft

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.ColorStyle
import com.google.api.services.sheets.v4.model.TextFormat
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.Command.Companion.convertColor
import de.tectoast.emolga.database.exposed.NameConventions
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.ResultStatProcessor
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.emolga.Nominations
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.StatLocation
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.slf4j.Logger

@Suppress("unused")
@Serializable
@SerialName("NDS")
class NDS : League() {

    val nominations: Nominations = Nominations(1, mutableMapOf())
    val sheetids: Map<String, Int> = mapOf()
    val teamnames: Map<Long, String> = mapOf()
    val teamtable: List<String> = emptyList()

    override fun isFinishedForbidden() = false

    @Transient
    override val timer = DraftTimer(TimerInfo(10, 22), 180)

    override fun beforePick(): String? {
        return "Du hast bereits 15 Mons!".takeIf { picks(current).count { it.name != "???" } == 15 }
    }

    override fun checkFinishedForbidden(mem: Long) = when {
        picks[mem]!!.filter { it.name != "???" }.size < 15 -> "Du hast noch keine 15 Pokemon!"
        !getPossibleTiers().values.all { it == 0 } -> "Du musst noch deine Tiers ausgleichen!"
        else -> null
    }

    override fun savePick(picks: MutableList<DraftPokemon>, pokemon: String, tier: String, free: Boolean) {
        picks.first { it.name == "???" }.apply {
            this.name = pokemon
            this.tier = tier
        }
    }

    override fun RequestBuilder.pickDoc(data: PickData) {
        doc(data)
    }

    override fun RequestBuilder.switchDoc(data: SwitchData) {
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        doc(data)
    }

    fun RequestBuilder.doc(data: DraftData) {
        val index = data.memIndex
        val y = index * 17 + 2 + data.changedIndex
        addSingle("Data!B$y", data.pokemon)
        addSingle("Data!AF$y", 2)
        addColumn(
            "Data!F${index * 17 + 2}",
            data.picks.asSequence().filter { it.name != "???" }
                .sortedWith(tierorderingComparator).map { NameConventions.convertOfficialToTL(it.name, guild)!! }
                .toList()
        )
        val numInRound = data.indexInRound + 1
        if (data is SwitchData) addSingle(
            "Draft!${Command.getAsXCoord(round * 5 - 3)}${numInRound * 5 + 1}", data.oldmon
        )
        addSingle("Draft!${Command.getAsXCoord(round * 5 - 1)}${numInRound * 5 + 2}", data.pokemon)
    }

    @Transient
    override val allowPickDuringSwitch = true

    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data", gameday + 6 + rrSummand, plindex * 17 + 2 + monindex
            )
        }
        deathProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data", gameday + 18 + rrSummand, plindex * 17 + 2 + monindex
            )
        }
        winProcessor = ResultStatProcessor { plindex: Int, gameday: Int ->
            StatLocation(
                "Data", gameday + 6 + rrSummand, plindex * 17 + 18
            )
        }
        looseProcessor = ResultStatProcessor { plindex: Int, gameday: Int ->
            StatLocation(
                "Data", gameday + 18 + rrSummand, plindex * 17 + 18
            )
        }
        resultCreator = {
            val y = index.y(10, 6)
            b.addSingle(
                "$gameplanName!${Command.getAsXCoord(gdi * 9 + 5)}${index * 10 + 4}",
                "=HYPERLINK(\"$url\"; \"Link\")"
            )
            b.addSingle(coord(gameplanName, gdi.x(9, 4), index.y(10, 3)), numberOne)
            b.addSingle(coord(gameplanName, gdi.x(9, 6), index.y(10, 3)), numberTwo)
            for (i in 0..1) {
                val x = gdi.x(9, i.y(8, 1))
                b.addColumn(coord(gameplanName, x, y), this.replayData.mons[i])
                b.addColumn(coord(gameplanName, gdi.x(9, i.y(4, 3)), y), kills[i])
                this.deaths[i].forEachIndexed { index, dead ->
                    if (dead) b.addCellFormatChange(
                        gameplanSheet,
                        "$x${y + index}",
                        deathFormat,
                        "textFormat(foregroundColorStyle,strikethrough)"
                    )
                }
            }
        }
        setStatIfEmpty = false
        sorterData = SorterData(
            formulaRange = listOf("$tableName!C3:K8".toDocRange(), "$tableName!C12:K17".toDocRange()),
            directCompare = true,
            indexer = { it.substring("=Data!F$".length).toInt() / 17 - 1 },
            cols = listOf(2, 8, -1)
        )
    }

    companion object {
        val logger: Logger by SLF4J
        private const val rr = false
        val rrSummand: Int
            get() = if (rr) 5 else 0
        val gameplanName: String
            get() = if (rr) "Spielplan RR" else "Spielplan HR"
        val gameplanSheet: Int
            get() = if (rr) 1634614187 else 453772599
        val tableName: String
            get() = if (rr) "Tabelle RR" else "Tabelle HR"

        private val deathFormat = CellFormat().apply {
            textFormat = TextFormat().apply {
                foregroundColorStyle = ColorStyle().apply {
                    rgbColor = convertColor(0x000000)
                }
                strikethrough = true
            }
        }
    }
}

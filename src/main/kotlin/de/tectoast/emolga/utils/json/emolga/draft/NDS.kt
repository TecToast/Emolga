package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.automation.structure.BasicResultCreator
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
    override val timer = DraftTimer.NDS

    override fun checkFinishedForbidden(mem: Long) =
        when {
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

    override fun pickDoc(data: PickData) {
        doc(data)
    }

    override fun switchDoc(data: SwitchData) {
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        doc(data)
    }

    private fun doc(data: DraftData) {
        val b = RequestBuilder(sid)
        val index = data.memIndex
        val y = index * 17 + 2 + data.changedIndex
        b.addSingle("Data!B$y", data.pokemon)
        b.addSingle("Data!AF$y", 2)
        b.addColumn("Data!F${index * 17 + 2}",
            data.picks.asSequence().filter { it.name != "???" }
                .sortedWith(compareBy({ tiers.indexOf(it.tier) }, { it.name })).map { it.name }.toList()
        )
        val numInRound = data.indexInRound + 1
        if (data is SwitchData) b.addSingle(
            "Draft!${Command.getAsXCoord(round * 5 - 3)}${numInRound * 5 + 1}",
            data.oldmon
        )
        b.addSingle("Draft!${Command.getAsXCoord(round * 5 - 1)}${numInRound * 5 + 2}", data.pokemon)
        b.execute()
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
        resultCreator = BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, _: Int, _: Int, url: String? ->
            b.addSingle(
                "Spielplan HR!${Command.getAsXCoord(gdi * 9 + 5)}${index * 10 + 4}", "=HYPERLINK(\"$url\"; \"Link\")"
            )
        }
        setStatIfEmpty = false
        sorterData = SorterData(
            listOf("Tabelle HR!C3:K8", "Tabelle HR!C12:K17"),
            true,
            { it.substring("=Data!F$".length).toInt() / 17 - 1 },
            2,
            8,
            -1
        )
    }

    companion object {
        val logger: Logger by SLF4J
        val tiers = listOf("S", "A", "B")
        const val rrSummand = 0
    }
}
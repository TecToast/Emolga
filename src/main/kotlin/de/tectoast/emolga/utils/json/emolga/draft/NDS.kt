package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.draft.PickData
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

    override fun checkFinishedForbidden(mem: Long) =
        if (picks[mem]!!.filter { it.name != "???" }.size < 15) "Du hast noch keine 15 Pokemon!" else null

    override fun savePick(picks: MutableList<DraftPokemon>, pokemon: String, tier: String, free: Boolean) {
        picks.first { it.name == "???" }.apply {
            this.name = pokemon
            this.tier = tier
        }
    }

    override fun pickDoc(data: PickData) {
        val b = RequestBuilder(sid)
        val mem = data.mem
        val teamname = teamnames[mem] ?: "ajdskjksadjkasdjaskdjaskdjaskdjas"
        val picks = picks.getValue(mem)
        val y = teamtable.indexOf(teamname) * 17 + 2 + data.changedIndex
        b.addSingle("Data!B$y", data.pokemon)
        b.addSingle("Data!AF$y", 2)
        val tiers = listOf("S", "A", "B")
        b.addColumn("Data!F${teamtable.indexOf(teamname) * 17 + 2}", picks
            .asSequence()
            .filter { it.name != "???" }
            .sortedWith(compareBy({ tiers.indexOf(it.tier) }, { it.name }))
            .map { it.name }
            .toList())
        val numInRound = data.indexInRound + 1
        b.addSingle("Draft!${Command.getAsXCoord(round * 5 - 1)}${numInRound * 5 + 2}", data.pokemon)
        logger.info("d.members.size() = ${members.size}")
        logger.info("d.order.size() = ${order.getValue(round).size}")
        logger.info("d.members.size() - d.order.size() = $numInRound")
        //if (d.members.size() - d.order.get(d.round).size() != 1 && isEnabled)
        b.execute()
    }

    @Transient
    override val docEntry = DocEntry.create {
        league = this@NDS
        killProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 6 + 5,
                plindex * 17 + 2 + monindex
            )
        }
        deathProcessor = BasicStatProcessor { plindex: Int, monindex: Int, gameday: Int ->
            StatLocation(
                "Data",
                gameday + 18 + 5,
                plindex * 17 + 2 + monindex
            )
        }
        winProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int ->
                StatLocation(
                    "Data",
                    gameday + 6 + 5,
                    plindex * 17 + 18
                )
            }
        looseProcessor =
            ResultStatProcessor { plindex: Int, gameday: Int ->
                StatLocation(
                    "Data",
                    gameday + 18 + 5,
                    plindex * 17 + 18
                )
            }
        resultCreator = BasicResultCreator { b: RequestBuilder, gdi: Int, index: Int, _: Int, _: Int, url: String? ->
            b.addSingle(
                "Spielplan RR!${Command.getAsXCoord(gdi * 9 + 5)}${index * 10 + 4}",
                "=HYPERLINK(\"$url\"; \"Link\")"
            )
        }
        setStatIfEmpty = false
        sorterData = SorterData(
            listOf("Tabelle RR!C3:K8", "Tabelle RR!C12:K17"),
            true, { it.substring("=Data!F$".length).toInt() / 17 - 1 }, 2, 8, -1
        )
    }

    companion object {
        val logger: Logger by SLF4J
    }
}
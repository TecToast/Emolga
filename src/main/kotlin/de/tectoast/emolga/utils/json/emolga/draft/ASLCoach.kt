package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.coord
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.ResultStatProcessor
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.records.StatLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ASLCoach")
class ASLCoach(val level: Int = -1, private val sheetid: Int = -1) : League() {
    @Transient
    override val docEntry = DocEntry.create(this) {
        val sheet = "Data$level"
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation(
                sheet,
                gameday + 2,
                plindex.y(15, monindex + 3)
            )
        }
        deathProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation(
                sheet,
                gameday + 12,
                plindex.y(15, monindex + 3)
            )
        }
        numberMapper = { it.ifEmpty { "-" } }
        winProcessor =
            ResultStatProcessor { plindex, gameday -> StatLocation(sheet, gameday + 2, plindex.y(15, 15)) }
        looseProcessor =
            ResultStatProcessor { plindex, gameday -> StatLocation(sheet, gameday + 12, plindex.y(15, 15)) }
        resultCreator = {
            b.addSingle(
                coord("Spielplan", gdi.x(5, 2), index.y(7, 5 + level)),
                "=HYPERLINK(\"$url\"; \"$numberOne:$numberTwo\")"
            )
        }
        //sorterData = SorterData(listOf("Tabellen!B5:J10", "Tabellen!B13:J18"), false, null, 2, 8, 6)

    }

    override val teamsize = 12

    @Transient
    override val timer = DraftTimer(TimerInfo(12, 22), 120)

    override val timerSkipMode = TimerSkipMode.LAST_ROUND

    override fun isFinishedForbidden() = false

    @Transient
    val comparator: Comparator<DraftPokemon> = compareBy({ it.tier.indexedBy(tierlist.order) }, { it.name })

    override fun RequestBuilder.pickDoc(data: PickData) {
        val asl = Emolga.get.asls11
        val (level, index, team) = asl.indexOfMember(data.mem)
        addSingle("Data$level!B${index.y(15, data.changedIndex + 3)}", data.pokemon)
        addColumn("$team!C${level.y(26, 23)}", data.picks.let { pi ->
            pi.sortedWith(comparator).map { it.indexedBy(pi) }.map { "=Data$level!B${index.y(15, 3) + it}" }
        })
        addSingle(
            "Draftreihenfolge ${
                when (level) {
                    0 -> "Coaches"
                    else -> "Stufe $level"
                }
            }!${data.round.minus(1).x(2, 2)}${data.indexInRound + 3}", data.pokemon
        )
    }

    override fun RequestBuilder.switchDoc(data: SwitchData) {
        val killY = level.y(12 * 12, 1000 + data.mem.indexedBy(table).y(12, data.changedIndex))
        addCopyPasteChange(343863794, "B$killY:F$killY", "B$killY", "PASTE_VALUES")
            .withRunnable {
                val b = builder()
                // Killliste neues Mon hinzufügen, altes Mon in Ablage cutpasten und neues Mon in Kader einfügen, Kaderseite sortieren
                val asl = Emolga.get.asls11
                val rowFirst = data.mem.indexedBy(table).y(15, 0)
                val row = rowFirst + data.changedIndex + 3
                b.addRow(
                    "Data!A${asl.yeetedmons.values.flatten().count() + 1731}", listOf(
                        data.pokemon,
                        "=Data$level!K$row",
                        "=WENNFEHLER(RUNDEN(Data$level!K$row / Data$level!V$row; 2); 0)",
                        "=Data$level!K$row - Data$level!U$row",
                        "=Data$level!V$row",
                        "=Data$level!U$row",
                        "=Data$level!B${rowFirst + 16}",
                        "Stufe $level"
                    )
                )
                val (level, index, team) = asl.indexOfMember(data.mem)
                val yeeted = asl.yeetedmons.getOrPut(data.mem) { mutableListOf() }
                val rowold = yeeted.size + rowFirst + 203
                yeeted.add(DraftPokemon(data.oldmon, data.oldtier))
                b.addSingle("Data$level!B$row", data.pokemon)
                b.addSingle("Data$level!B${rowold}", data.oldmon)
                b.addCutPasteChange(sheetid, "C$row:G$row", "C$rowold", "PASTE_VALUES")
                b.addCutPasteChange(sheetid, "M$row:Q$row", "M$rowold", "PASTE_VALUES")
                b.addColumn("$team!C${level.y(26, 23)}", data.picks.let { pi ->
                    pi.sortedWith(comparator).map { it.indexedBy(pi) }.map { "=Data$level!B${index.y(15, 3) + it}" }
                })
                b.addColumn("$team!C${level.y(26, 35)}", yeeted.let { pi ->
                    pi.sortedWith(comparator).map { it.indexedBy(pi) }.map { "=Data$level!B${index.y(15, 203) + it}" }
                })
                val sheetname = "Zwischendraftreihenfolge ${
                    when (level) {
                        0 -> "Coaches"
                        else -> "Stufe $level"
                    }
                }"
                b.addSingle("$sheetname!${data.round.minus(1).x(3, 2)}${data.indexInRound + 3}", data.oldmon)
                b.addSingle("$sheetname!${data.round.minus(1).x(3, 3)}${data.indexInRound + 3}", data.pokemon)
                b.execute()
            }
    }

    override fun announcePlayer() {
        tc.sendMessage("${getCurrentMention()} ist dran! (${points[current]} mögliche Punkte)")
            .queue()
    }

    override fun getCurrentMention() = "<@$current> (<@&${Emolga.get.asls11.roleIdByMember(current)}>)"

    override fun isCurrent(user: Long): Boolean {
        return user in Emolga.get.asls11.teammembersByMember(current)
    }
}


// Dasor steht hier, weil er das so wollte

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
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import de.tectoast.emolga.utils.records.Coord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("ASLCoach")
class ASLCoach(val level: Int = -1) : League() {
    @Transient
    override val docEntry = DocEntry.create(this) {
        val sheet = "Data$level"
        killProcessor = BasicStatProcessor {
            Coord(
                sheet,
                gameday + 2,
                plindex.y(15, monindex + 3)
            )
        }
        deathProcessor = BasicStatProcessor {
            Coord(
                sheet,
                gameday + 12,
                plindex.y(15, monindex + 3)
            )
        }
        numberMapper = { it.ifEmpty { "-" } }
        winProcessor =
            ResultStatProcessor { Coord(sheet, gameday + 2, plindex.y(15, 15)) }
        looseProcessor =
            ResultStatProcessor { Coord(sheet, gameday + 12, plindex.y(15, 15)) }
        resultCreator = {
            b.addSingle(
                coord("Spielplan", gdi.x(5, 2), index.y(7, 5 + level)),
                defaultGameplanString
            )
        }
        //sorterData = SorterData(listOf("Tabellen!B5:J10", "Tabellen!B13:J18"), false, null, 2, 8, 6)

    }

    override val teamsize = 12

    @Transient
    override val timer = DraftTimer(TimerInfo(12, 22))

    override val timerSkipMode = TimerSkipMode.AFTER_DRAFT_UNORDERED

    override fun isFinishedForbidden() = false

    @Transient
    val comparator: Comparator<DraftPokemon> = compareBy({ it.tier.indexedBy(tierlist.order) }, { it.name })

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        val asl = db.aslcoach.only()
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

    override suspend fun announcePlayer() {
        tc.sendMessage("${getCurrentMention()} ist dran! (${points[current]} mögliche Punkte)")
            .queue()
    }

    override suspend fun getCurrentMention() = "<@$current> ||<@&${db.aslcoach.only().roleIdByMember(current)}||)"

    override suspend fun isCurrent(user: Long): Boolean {
        return user in db.aslcoach.only().teammembersByMember(current)
    }
}


// Dasor steht hier, weil er das so wollte

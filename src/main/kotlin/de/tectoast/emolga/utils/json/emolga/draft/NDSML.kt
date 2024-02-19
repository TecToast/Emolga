package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.repeat.RepeatTask
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.litote.kmongo.eq
import org.slf4j.Logger
import kotlin.time.Duration.Companion.days

@Serializable
@SerialName("NDSML")
class NDSML : League() {
    override val teamsize = 11
    override val pickBuffer = 6

    @Transient
    override var timer: DraftTimer? =
        SimpleTimer(TimerInfo(mapOf(0 to 180, 1 to 150, 2 to 120, 3 to 90, 4 to 60)).set(10, 22))

    override val duringTimerSkipMode = NEXT_PICK
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED
    val logger: Logger by SLF4J

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(SorterData("Tabelle!C3:K12", newMethod = true, cols = listOf(2, 8, 6))) {
            val y = index.y(10, 6)
            val gameplanName = "Spielplan"
            val gameplanSheet = 453772599
            val teamnames =
                db.signups.get(Constants.G.NDS)!!.users.toList().associate { it.first to it.second.teamname!! }
            b.addSingle(
                "$gameplanName!${getAsXCoord(gdi * 9 + 5)}${index * 10 + 4}", "=HYPERLINK(\"$url\"; \"Link\")"
            )
            b.addSingle(coord(gameplanName, gdi.x(9, 4), index.y(10, 3)), numberOne)
            b.addSingle(coord(gameplanName, gdi.x(9, 6), index.y(10, 3)), numberTwo)
            for (i in 0..1) {
                val x = gdi.x(9, i.y(8, 1))
                val dataI = i.swap()
                logger.info("i: $i")
                logger.info("dataI: $dataI")
                b.addColumn(
                    coord(gameplanName, x, y),
                    this.replayData.mons[dataI].map { NameConventionsDB.convertOfficialToTL(it, guild)!! })
                b.addColumn(coord(gameplanName, gdi.x(9, i.y(4, 3)), y), kills[dataI])
                this.deaths[dataI].forEachIndexed { index, dead ->
                    if (dead) b.addCellFormatChange(
                        gameplanSheet,
                        "$x${y + index}",
                        NDS.deathFormat,
                        "textFormat(foregroundColorStyle,strikethrough)"
                    )
                }
                if (winnerIndex == i) {
                    val s = "!${(gdi * 2 + 4).xc()}10"
                    b.addSingle(teamnames[replayData.uids[i]] + s, "$higherNumber:0")
                    b.addSingle(teamnames[replayData.uids[1 - i]] + s, "0:$higherNumber")
                }
            }
        }
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle("Data!AA${data.memIndex.y(newSystemGap, data.changedOnTeamsiteIndex + 3)}", data.pokemon)
        addSingle("Draftreihenfolge!${data.roundIndex.x(2, 2)}${data.indexInRound.y(2, 5)}", data.pokemon)
    }

    companion object {
        fun setupRepeatTasks() {
            RepeatTask(
                "28.01.2024 20:00", 9, 7.days, true
            ) { doMatchUps(it) }
        }

        suspend fun doMatchUps(gameday: Int) {
            val ndsml = db.drafts.findOne(League::leaguename eq "NDSML")!!
            val table = ndsml.table
            val teamnames =
                db.signups.get(Constants.G.NDS)!!.users.toList().associate { it.first to it.second.teamname!! }
            val battleorder = ndsml.battleorder[gameday]!!
            val b = RequestBuilder(ndsml.sid)
            for (users in battleorder) {
                for (index in 0..1) {
                    println(users)
                    val u1 = table[users[index]]
                    val oppoIndex = users[1 - index]
                    val u2 = table[oppoIndex]
                    val team = teamnames[u1]!!
                    val oppo = teamnames[u2]!!
                    // Speed values
                    b.addSingle("$team!B17", "={${Coord(oppo, "B15").spreadTo(x = 11 * 2)}}")
                    // Icons
                    b.addSingle("$team!B18", "={${Coord(oppo, "B14").spreadTo(x = 11 * 2)}}")
                    // KD
                    b.addSingle("$team!B20", "={${Coord(oppo, "B13").spreadTo(x = 11 * 2)}}")
                    // MU
                    b.addColumn(
                        "$team!A17", listOf(
                            "=${Coord(oppo, "Y2")}", "=${Coord(oppo, "B2")}"
                        )
                    )


                }
            }
            b.execute()
        }
    }
}

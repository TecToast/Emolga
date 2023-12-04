package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.defaultTimeFormat
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.repeat.RepeatTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.litote.kmongo.eq
import java.time.Duration

@Serializable
@SerialName("NDSML")
class NDSML : League() {
    override val teamsize = 11
    override val pickBuffer = 6

    @Transient
    override val timer = DraftTimer(TimerInfo(mapOf(0 to 180, 1 to 150, 2 to 120, 3 to 90, 4 to 60)).set(10, 22))

    override val duringTimerSkipMode = NEXT_PICK
    override val afterTimerSkipMode = AFTER_DRAFT_UNORDERED

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle("Data!AA${data.memIndex.y(newSystemGap, data.changedOnTeamsiteIndex + 3)}", data.pokemon)
        addSingle("Draftreihenfolge!${data.roundIndex.x(2, 2)}${data.indexInRound.y(2, 5)}", data.pokemon)
    }

    companion object {
        fun setupRepeatTasks() {
            RepeatTask(
                defaultTimeFormat.parse("28.01.2024 20:00").toInstant(), 9, Duration.ofDays(7L), true
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
                    b.addSingle("$team!B17", "={${Coord(oppo, "B", 15).spreadTo(x = 11 * 2)}}")
                    // Icons
                    b.addSingle("$team!B18", "={${Coord(oppo, "B", 14).spreadTo(x = 11 * 2)}}")
                    // KD
                    b.addSingle("$team!B20", "={${Coord(oppo, "B", 13).spreadTo(x = 11 * 2)}}")
                    // MU
                    b.addColumn(
                        "$team!A17", listOf(
                            "=${Coord(oppo, "Y", 2)}", "=${Coord(oppo, "B", 2)}"
                        )
                    )


                }
            }
            b.execute()
        }
    }
}

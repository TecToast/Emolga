package de.tectoast.emolga.utils.json.emolga.draft

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.ColorStyle
import com.google.api.services.sheets.v4.model.TextFormat
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.Command.Companion.convertColor
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import de.tectoast.emolga.utils.automation.structure.BasicStatProcessor
import de.tectoast.emolga.utils.automation.structure.DocEntry
import de.tectoast.emolga.utils.automation.structure.ResultStatProcessor
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.Nominations
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.StatLocation
import de.tectoast.toastilities.repeat.RepeatTask
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.slf4j.Logger
import java.time.Duration

@Suppress("unused")
@Serializable
@SerialName("NDS")
class NDS : League() {

    val nominations: Nominations = Nominations(1, mutableMapOf())
    val sheetids: Map<String, Int> = mapOf()
    val teamnames: Map<Long, String> = mapOf()
    val teamtable: List<String> = emptyList()

    override fun isFinishedForbidden() = false
    override val teamsize = 15
    override val pickBuffer = -1

    @Transient
    override val timer = DraftTimer(TimerInfo(10, 22), 3 * 60)

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

    override fun saveSwitch(picks: MutableList<DraftPokemon>, oldmon: String, newmon: String, newtier: String): Int {
        val index = picks.sortedWith(tierorderingComparator).indexOfFirst { it.name == oldmon }
        picks.first { it.name == oldmon }.apply {
            this.name = newmon
            this.tier = newtier
        }
        return index
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        doc(data)
    }

    override suspend fun RequestBuilder.switchDoc(data: SwitchData) {
        //logger.info(d.order.get(d.round).stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")));
        doc(data)
    }

    private suspend fun RequestBuilder.doc(data: DraftData) {
        val index = data.memIndex
        val y = index * 17 + 2 + data.changedIndex
        addSingle("Data!B$y", data.pokemon)
        addSingle("Data!AF$y", 2)
        addColumn(
            "Data!F${index * 17 + 2}",
            data.picks.filter { it.name != "???" }.sortedWith(tierorderingComparator)
                .map { NameConventionsDB.convertOfficialToTL(it.name, guild)!! }.toList()
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
                "$gameplanName!${Command.getAsXCoord(gdi * 9 + 5)}${index * 10 + 4}", "=HYPERLINK(\"$url\"; \"Link\")"
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
                        gameplanSheet, "$x${y + index}", deathFormat, "textFormat(foregroundColorStyle,strikethrough)"
                    )
                }
                if (winnerIndex == i) {
                    val s = "!${(gdi.plus(rrSummand) * 2 + 4).xc()}10"
                    b.addSingle(teamnames[replayData.uids[i]] + s, "$higherNumber:0")
                    b.addSingle(teamnames[replayData.uids[1 - i]] + s, "0:$higherNumber")
                }
            }

        }
        setStatIfEmpty = false
        sorterData = SorterData(
            formulaRange = listOf("$tableName!C3:K8".toDocRange(), "$tableName!C12:K17".toDocRange()),
            directCompare = true,
            newMethod = true,
            cols = listOf(2, 8, -1, 6)
        )
    }

    override fun onTipGameLockButtons(gameday: Int) {
        val map =
            (tipgame?.tips?.get(gameday) ?: return).userdata.values.flatMap { it.values }.groupingBy { it }.eachCount()
        RequestBuilder(sid).addColumn("TipGameData!${(gameday + rrSummand + 3).xc()}2", (0..11).map { map[it] ?: 0 })
            .execute()
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

        suspend fun doMatchUps(gameday: Int, withAnnounce: Boolean = false) {
            val nds = db.nds()
            val table = nds.table
            val teamnames = nds.teamnames
            val battleorder = nds.battleorder[gameday]!!
            val b = RequestBuilder(nds.sid)
            val tipgameStats = mutableListOf<String>()
            for (users in battleorder) {
                for (index in 0..1) {
                    println(users)
                    val u1 = table[users[index]]
                    val oppoIndex = users[1 - index]
                    val u2 = table[oppoIndex]
                    val team = teamnames[u1]!!
                    val oppo = teamnames[u2]!!
                    tipgameStats += "='$team'!Y2"
                    // Speed values
                    b.addSingle("$team!B18", "={'$oppo'!B16:AE16}")
                    // Icons
                    b.addSingle("$team!B19", "={'$oppo'!B15:AE15}")
                    // KD
                    b.addSingle("$team!B21", "={'$oppo'!B14:AF14}")
                    // MU
                    b.addColumn(
                        "$team!A18", listOf(
                            "='$oppo'!Y2", "='$oppo'!B2"
                        )
                    )
                    // Z & Tera
                    b.addSingle("$team!AE8", "='$oppo'!AA8")
                    b.addSingle("$team!AE10", "='$oppo'!AA10")
                    b.addSingle("$team!AC11", "='$oppo'!Y11")
                    // Conditional formatting tiers
                    val y = oppoIndex * 2 + 300
                    b.addSingle("$team!B49", "={Data!B$y:AE$y}")

                }
            }
            b.addColumn("TipGameData!N16", tipgameStats)
            b.addSingle("TipGameData!N29", gameday + rrSummand)
            if (withAnnounce) {
                b.withRunnable {
                    EmolgaMain.emolgajda.getTextChannelById(837425690844201000L)!!.sendMessage(
                        "Jo, kurzer Reminder, die Matchups des nächsten Spieltages sind im Doc, vergesst das Nominieren nicht :)\n<@&856205147754201108>"
                    ).queue()
                }
            }
            b.execute()
        }


        suspend fun doNDSNominate(prevDay: Boolean = false, vararg onlySpecifiedUsers: Long) {
            val nds = db.nds()
            val nom = nds.nominations
            val table = nds.table
            var cday = nom.currentDay
            if (prevDay) cday--
            val o = nom.nominated[cday]!!
            val picks = nds.picks
            val sid = nds.sid
            val b = RequestBuilder(sid)
            val tiers = listOf("S", "A", "B")
            var dbcallTime = 0L
            for (u in onlySpecifiedUsers.takeIf { it.isNotEmpty() }?.toList() ?: picks.keys) {
                //String u = "297010892678234114";
                val pmons: MutableList<DraftPokemon> = picks[u]!!
                if (u !in o) {
                    if (cday == 1) {
                        val comp = compareBy<DraftPokemon>({ it.tier.indexedBy(tiers) }, { it.name })
                        o[u] = (buildString {
                            append(pmons.sortedWith(comp).joinToString(";") { it.indexedBy(pmons).toString() })
                        })
                    } else {
                        o[u] = nom.nominated[cday - 1]!![u]!!
                    }
                }
                //logger.info("o.get(u) = " + o.get(u));
                val str = o[u]!!
                val start = System.currentTimeMillis()
                val mons: List<String> =
                    str.split(";").map { NameConventionsDB.convertOfficialToTL(pmons[it.toInt()].name, nds.guild)!! }
                dbcallTime += System.currentTimeMillis() - start
                logger.info("mons = $mons")
                logger.info("u = $u")
                val index = table.indexOf(u)
                b.addColumn("Data!F${index.y(17, 2)}", mons)
            }
            b.withRunnable {
                if (onlySpecifiedUsers.isEmpty()) {
                    EmolgaMain.emolgajda.getTextChannelById(837425690844201000L)!!.sendMessage(
                        """
                Guten Abend ihr Teilnehmer. Der nächste Spieltag öffnet seine Pforten...Was? Du hast vergessen zu nominieren? Dann hast du wieder mal Pech gehabt. Ab jetzt könnt ihr euch die Nominierungen im Dokument anschauen und verzweifelt feststellen, dass ihr völlig lost gewesen seid bei eurer Entscheidung hehe. Wie dem auch sei, viel Spaß beim Teambuilding. Und passt auf Maxis Mega-Gewaldro auf! Warte, er hat keins mehr? Meine ganzen Konstanten im Leben wurden durchkreuzt...egal, wir hören uns nächste Woche wieder!
_written by Maxifcn_""".trimIndent()
                    ).queue()
                }
            }.execute()
            logger.info("dbcallTime = $dbcallTime")
            if (!prevDay) nom.currentDay++
            nds.save()
        }

        fun setupRepeatTasks() {
            RepeatTask(
                defaultTimeFormat.parse("20.08.2023 20:00").toInstant(),
                5,
                Duration.ofDays(7L),
                { runBlocking { doMatchUps(it, withAnnounce = true) } },
                true
            )
            RepeatTask(
                defaultTimeFormat.parse("23.08.2023 00:00").toInstant(),
                5,
                Duration.ofDays(7L),
                { runBlocking { doNDSNominate() } },
                true
            )
        }
    }
}

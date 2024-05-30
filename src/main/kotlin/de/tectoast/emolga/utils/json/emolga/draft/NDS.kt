package de.tectoast.emolga.utils.json.emolga.draft

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.ColorStyle
import com.google.api.services.sheets.v4.model.TextFormat
import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.DynamicCoord
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.Nominations
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import de.tectoast.emolga.utils.y
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.slf4j.Logger
import kotlin.time.Duration.Companion.days

@Suppress("unused")
@Serializable
@SerialName("NDS")
class NDS(val rr: Boolean) : League() {


    val nominations: Nominations = Nominations(1, mutableMapOf())
    val sheetids: Map<String, Int> = mapOf()
    val teamtable: List<String> = emptyList()

    init {
        enableConfig(AllowPickDuringSwitch)
        val z = TZDataHolder(
            DynamicCoord.DynamicSheet(teamtable, "AA8"),
            "Data!\$B\$2000:\$H\$3000",
            6
        )
        val mon = z.copy(coord = DynamicCoord.DynamicSheet(teamtable, "AA10"))
        val type = TZDataHolder(
            DynamicCoord.DynamicSheet(teamtable, "Y11"),
            "Data!\$B\$400:\$C$417",
            2
        )
        enableConfig(
            TeraAndZ(
                z = z,
                tera = TeraData(
                    mon = mon,
                    type = type
                )
            )
        )
    }

    override fun isFinishedForbidden() = false
    override val teamsize = 15
    override val pickBuffer = 12

    @Transient
    override var timer: DraftTimer? = SimpleTimer(TimerInfo(10, 22, delayInMins = 3 * 60))

    @Transient
    override val additionalSet = AdditionalSet("D", "X", "Y")

    fun getTeamname(uid: Long) = teamtable[table.indexOf(uid)]

    override fun beforePick(): String? {
        return "Du hast bereits 15 Mons!".takeIf { picks(current).count { !it.quit } == 15 }
    }

    override fun checkFinishedForbidden(mem: Long) = when {
        picks[mem]!!.filter { !it.quit }.size < 15 -> "Du hast noch keine 15 Pokemon!"
        !getPossibleTiers().values.all { it == 0 } -> "Du musst noch deine Tiers ausgleichen!"
        else -> null
    }

    override fun saveSwitch(picks: MutableList<DraftPokemon>, oldmon: String, newmon: String, newtier: String): Int {
        if (rr) {
            return super.saveSwitch(picks, oldmon, newmon, newtier)
        }
        picks.first { it.name == oldmon }.apply {
            this.name = newmon
            this.tier = newtier
        }
        return -1
    }

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        doc(data)
    }

    override suspend fun RequestBuilder.switchDoc(data: SwitchData) {
        doc(data)
    }

    private suspend fun RequestBuilder.doc(data: DraftData) {
        val isSwitch = data is SwitchData
        val y = newSystemPickDoc(data, if (rr) data.picks.size - 1 else data.changedIndex)
        if (isSwitch && rr) {
            additionalSet.let {
                addSingle("$dataSheet!${it.col}${data.memIndex.y(newSystemGap, data.oldIndex + 3)}", it.yeeted)
            }
        }
        addSingle("Data!AF$y", 2)
        addColumn(
            "Data!F${data.memIndex * 30 + 3}",
            data.picks.filter { !it.quit }.sortedWith(tierorderingComparator)
                .map { NameConventionsDB.convertOfficialToTL(it.name, guild)!! }.toList()
        )
        val numInRound = data.indexInRound + 1
        if (isSwitch) addSingle(
            "Draft!${getAsXCoord(round * 5 - 3)}${numInRound * 5 + 1}", data.oldmon
        )
        addSingle("Draft!${getAsXCoord(round * 5 - 1)}${numInRound * 5 + 2}", data.pokemon)
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        killProcessor = BasicStatProcessor {
            Coord(
                "Data", gameday + 6, plindex * 30 + 3 + monindex
            )
        }
        deathProcessor = BasicStatProcessor {
            Coord(
                "Data", gameday + 18, plindex * 30 + 3 + monindex
            )
        }
        winProcessor = ResultStatProcessor {
            Coord(
                "Data", gameday + 6, plindex * 30 + 30
            )
        }
        looseProcessor = ResultStatProcessor {
            Coord(
                "Data", gameday + 18, plindex * 30 + 30
            )
        }
        resultCreator = {
            val y = index.y(10, 6)
            val normedGdi = gdi - rrSummand
            b.addSingle(
                "$gameplanName!${getAsXCoord(normedGdi * 9 + 5)}${index * 10 + 4}", "=HYPERLINK(\"$url\"; \"Link\")"
            )
            b.addSingle(coord(gameplanName, normedGdi.x(9, 4), index.y(10, 3)), numberOne)
            b.addSingle(coord(gameplanName, normedGdi.x(9, 6), index.y(10, 3)), numberTwo)
            for (i in 0..1) {
                val x = normedGdi.x(9, i.y(8, 1))
                val dataI = i.swap()
                logger.info("i: $i")
                logger.info("dataI: $dataI")
                b.addColumn(
                    coord(gameplanName, x, y),
                    this.replayData.mons[dataI].map { NameConventionsDB.convertOfficialToTL(it, guild)!! })
                b.addColumn(coord(gameplanName, normedGdi.x(9, i.y(4, 3)), y), kills[dataI])
                this.deaths[dataI].forEachIndexed { index, dead ->
                    if (dead) b.addCellFormatChange(
                        gameplanSheet, "$x${y + index}", deathFormat, "textFormat(foregroundColorStyle,strikethrough)"
                    )
                }
                if (winnerIndex == i) {
                    val s = "!${(gdi * 2 + 4).xc()}10"
                    b.addSingle(getTeamname(replayData.uids[i]) + s, "$higherNumber:0")
                    b.addSingle(getTeamname(replayData.uids[1 - i]) + s, "0:$higherNumber")
                }
            }

        }
        setStatIfEmpty = false
        sorterData = SorterData(
            formulaRange = listOf("$tableName!C3:K8", "$tableName!C12:K17"),
            newMethod = true,
            cols = listOf(2, 8, -1, 6)
        )
    }

    val rrSummand: Int
        get() = if (rr) 5 else 0
    val gameplanName: String
        get() = if (rr) "Spielplan RR" else "Spielplan HR"
    val gameplanSheet: Int
        get() = if (rr) 1634614187 else 453772599
    val tableName: String
        get() = if (rr) "Tabelle RR" else "Tabelle HR"

    override fun setupRepeatTasks() {
        logger.info("Setting up matchups repeat tasks")
        RepeatTask(
            "NDS", RepeatTaskType.Other("Matchups"), "05.05.2024 20:00", 10, 7.days
        ) { doMatchUps(it, withAnnounce = true) }
        logger.info("Setting up nominations repeat tasks")
        RepeatTask(
            "NDS", RepeatTaskType.Other("Nominate"), "08.05.2024 00:00", 10, 7.days
        ) { doNDSNominate() }
    }

    companion object {
        val logger: Logger by SLF4J

        val deathFormat = CellFormat().apply {
            textFormat = TextFormat().apply {
                foregroundColorStyle = ColorStyle().apply {
                    rgbColor = 0x000000.convertColor()
                }
                strikethrough = true
            }
        }

        suspend fun doMatchUps(gameday: Int, withAnnounce: Boolean = false) {
            val nds = db.nds()
            val table = nds.table
            val battleorder = nds.battleorder[gameday]!!
            val b = RequestBuilder(nds.sid)
            val tipgameStats = mutableListOf<String>()
            for (users in battleorder) {
                for (index in 0..1) {
                    val u1 = table[users[index]]
                    val oppoIndex = users[1 - index]
                    val u2 = table[oppoIndex]
                    val team = nds.getTeamname(u1)
                    val oppo = nds.getTeamname(u2)
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
                    val y = oppoIndex * 2 + 500
                    b.addSingle("$team!B49", "={Data!B$y:AE$y}")
                }
            }
            b.execute()
            if (withAnnounce) {
                b.withRunnable {
                    jda.getTextChannelById(837425690844201000L)!!.sendMessage(
                        "Jo, kurzer Reminder, die Matchups des nächsten Spieltages sind im Doc, vergesst das Nominieren nicht :)\n<@&856205147754201108>"
                    ).queue()
                }
            }
        }


        suspend fun doNDSNominate(prevDay: Boolean = false, withSend: Boolean = true, vararg onlySpecifiedUsers: Long) {
            val nds = db.nds()
            val nom = nds.nominations
            var cday = nom.currentDay
            if (prevDay) cday--
            val dayData = nom.nominated[cday]!!
            val picks = nds.picks
            val b = RequestBuilder(nds.sid)
            var dbcallTime = 0L
            for (u in onlySpecifiedUsers.takeIf { it.isNotEmpty() }?.toList() ?: picks.keys) {
                val pmons = picks[u]!!
                if (u !in dayData) {
                    dayData[u] = if (cday == 1) {
                        pmons.sortedWith(nds.tierorderingComparator)
                            .map { it.indexedBy(pmons) }
                    } else nom.nominated[cday - 1]!![u]!!
                }
                val indices = dayData[u]!!
                val start = System.currentTimeMillis()
                val mons = indices.map { NameConventionsDB.convertOfficialToTL(pmons[it].name, nds.guild)!! }
                dbcallTime += System.currentTimeMillis() - start
                logger.info("mons = $mons")
                logger.info("u = $u")
                val index = nds.table.indexOf(u)
                b.addColumn("Data!F${index.y(30, 3)}", mons)
            }
            b.withRunnable {
                if (onlySpecifiedUsers.isEmpty() && withSend) {
                    jda.getTextChannelById(837425690844201000L)!!.sendMessage(
                        """
                Guten Abend ihr Teilnehmer. Der nächste Spieltag öffnet seine Pforten...Was? Du hast vergessen zu nominieren? Dann hast du wieder mal Pech gehabt. Ab jetzt könnt ihr euch die Nominierungen im Dokument anschauen und verzweifelt feststellen, dass ihr völlig lost gewesen seid bei eurer Entscheidung hehe. Wie dem auch sei, viel Spaß beim Teambuilding. Und passt auf Maxis Mega-Gewaldro auf! Warte, er hat keins mehr? Meine ganzen Konstanten im Leben wurden durchkreuzt...egal, wir hören uns nächste Woche wieder!
_written by Maxifcn_""".trimIndent()
                    ).queue()
                }
            }.execute()
            logger.info("dbcallTime = $dbcallTime")
            if (!prevDay) nom.currentDay++
            nds.save("Nominate RepeatTask")
        }
    }
}

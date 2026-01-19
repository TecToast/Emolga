package de.tectoast.emolga.league

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.ColorStyle
import com.google.api.services.sheets.v4.model.TextFormat
import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.CombinedOptionsPriceManager
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.Nominations
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.TableSortOption
import de.tectoast.emolga.utils.records.newSystemSorter
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
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

    override val duringTimerSkipMode = NEXT_PICK
    val nominations: Nominations = Nominations(1, mutableMapOf())
    val sheetids: Map<String, Int> = mapOf()
    val teamtable: List<String> = emptyList()

    override fun isFinishedForbidden() = false
    override val teamsize = 15
    override val pickBuffer = 12

    @Transient
    override val additionalSet = AdditionalSet("D", "X", "Y")

    fun getTeamname(idx: Int) = teamtable[idx]

    override fun checkFinishedForbidden(idx: Int) = when {
        picks[idx]!!.filter { !it.quit }.size < 15 -> "Du hast noch keine 15 Pokemon!"
        tierlist.withPriceManager<CombinedOptionsPriceManager, List<Map<String, Int>>> {
            it.getAllPossibleTiers().filter { map -> map.values.all { v -> v >= 0 } }
        }?.all { tiers -> tiers.values.all { it == 0 } } == false -> "Du musst noch deine Tiers ausgleichen!"

        else -> null
    }

    override fun SwitchData.saveSwitch() {
        if (rr) {
            // super implementation, currently not possible to call directly
            picks.first { it.name == oldmon.official }.quit = true
            picks += DraftPokemon(pokemonofficial, tier)
            return
        }
        picks.first { it.name == oldmon.official }.apply {
            this.name = pokemonofficial
            this.tier = this@saveSwitch.tier
        }
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
                addSingle("$dataSheet!${it.col}${data.idx.y(newSystemGap, data.oldIndex + 3)}", it.yeeted)
            }
        }
        addSingle("Data!AF$y", 2)
        addColumn(
            "Data!F${data.idx * 30 + 3}",
            data.picks.filter { !it.quit }.sortedWith(tierorderingComparator)
                .map { NameConventionsDB.convertOfficialToTL(it.name, guild)!! }.toList()
        )
        val numInRound = data.indexInRound + 1
        if (isSwitch) addSingle(
            "Draft!${getAsXCoord(round * 5 - 3)}${numInRound * 5 + 1}", data.oldmon.tlName
        )
        addSingle("Draft!${getAsXCoord(round * 5 - 1)}${numInRound * 5 + 2}", data.pokemon)
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        +StatProcessor {
            Coord(
                "Data", gdi + 7, memIdx * 30 + 3 + monIndex()
            ) to DataTypeForMon.KILLS
        }
        +StatProcessor {
            Coord(
                "Data", gdi + 19, memIdx * 30 + 3 + monIndex()
            ) to DataTypeForMon.DEATHS
        }
        +StatProcessor {
            Coord("Data", gdi + 7, memIdx * 30 + 30) to DataTypeForMon.WINS
        }
        +StatProcessor {
            Coord(
                "Data", gdi + 19, memIdx * 30 + 30
            ) to DataTypeForMon.LOSSES
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
                logger.info("i: $i")
                logger.info("dataI: $i")
                b.addColumn(
                    coord(gameplanName, x, y),
                    this.firstReplayData.kd[i].keys.map { NameConventionsDB.convertOfficialToTL(it, guild)!! })
                b.addColumn(coord(gameplanName, normedGdi.x(9, i.y(4, 3)), y), kills[i])
                this.deaths[i].forEachIndexed { index, dead ->
                    if (dead) b.addCellFormatChange(
                        gameplanSheet, "$x${y + index}", deathFormat, "textFormat(foregroundColorStyle,strikethrough)"
                    )
                }
                if (winnerIndex == i) {
                    val s = "!${(gdi * 2 + 4).xc()}10"
                    b.addSingle(getTeamname(fullGameData.uindices[i]) + s, "$higherNumber:0")
                    b.addSingle(getTeamname(fullGameData.uindices[1 - i]) + s, "0:$higherNumber")
                }
            }

        }
        setStatIfEmpty = false
        val sortOptions = TableSortOption.fromCols(listOf(2, 8, -1, 6))
        sorterDatas += newSystemSorter("$tableName!C3:K8", sortOptions)
        sorterDatas += newSystemSorter("$tableName!C12:K17", sortOptions)
    }

    val rrSummand: Int
        get() = if (rr) 5 else 0
    val gameplanName: String
        get() = if (rr) "Spielplan RR" else "Spielplan HR"
    val gameplanSheet: Int
        get() = if (rr) 1634614187 else 453772599
    val tableName: String
        get() = if (rr) "Tabelle RR" else "Tabelle HR"

    override fun setupCustomRepeatTasks() {
        logger.info("Setting up matchups repeat tasks")
        // TODO: Move to DB
        RepeatTask(
            "NDS", RepeatTaskType.Other("Matchups"), "7.12.2025 20:00", 10, 7.days
        ) { doMatchUps(it, withAnnounce = true) }
        logger.info("Setting up nominations repeat tasks")
        RepeatTask(
            "NDS", RepeatTaskType.Other("Nominate"), "10.12.2025 00:00", 10, 7.days
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
            nds.table
            val battleorder = nds.battleorder[gameday]!!
            val b = RequestBuilder(nds.sid)
            val tipgameStats = mutableListOf<String>()
            for (users in battleorder) {
                for (index in 0..1) {
                    val u1 = users[index]
                    val oppoIndex = users[1 - index]
                    val team = nds.getTeamname(u1)
                    val oppo = nds.getTeamname(oppoIndex)
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
                    b.addSingle("$team!AC8", "={'$oppo'!Y8:AB11}")
                    // Conditional formatting tiers
                    val y = oppoIndex * 2 + 500
                    b.addSingle("$team!B49", "={Data!B$y:AE$y}")
                }
            }
            if (withAnnounce) {
                b.withRunnable {
                    jda.getTextChannelById(837425690844201000L)!!.sendMessage(
                        "Jo, kurzer Reminder, die Matchups des nächsten Spieltages sind im Doc, vergesst das Nominieren nicht :)\n<@&1415057603696001154>"
                    ).queue()
                }
            }
            b.execute()
        }


        suspend fun doNDSNominate(prevDay: Boolean = false, withSend: Boolean = true, vararg onlySpecifiedUsers: Int) {
            executeOnFreshLock({ db.nds() }) {
                val nds = this as NDS
                val nom = nds.nominations
                var cday = nom.currentDay
                if (prevDay) cday--
                val dayData = nom.nominated[cday]!!
                val picks = nds.picks
                val b = RequestBuilder(nds.sid)
                var dbcallTime = 0L
                for (u in onlySpecifiedUsers.takeIf { it.isNotEmpty() }?.toList() ?: nds.table.indices) {
                    val pmons = picks[u]!!.filter { !it.quit }
                    if (u !in dayData) {
                        dayData[u] = if (cday % 5 == 1) {
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
                    b.addColumn("Data!F${u.y(30, 3)}", mons)
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
                nds.save()
            }
        }
    }
}

package de.tectoast.emolga.utils.automation.structure

import de.tectoast.emolga.commands.Command.Companion.compareColumns
import de.tectoast.emolga.commands.Command.Companion.getNumber
import de.tectoast.emolga.commands.Command.Companion.indexPick
import de.tectoast.emolga.commands.ReplayData
import de.tectoast.emolga.commands.indexedBy
import de.tectoast.emolga.commands.names
import de.tectoast.emolga.commands.y
import de.tectoast.emolga.database.exposed.TipGamesDB
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class DocEntry private constructor(val league: League) {
    companion object {
        private val logger = LoggerFactory.getLogger(DocEntry::class.java)

        fun create(league: League, builder: DocEntry.() -> Unit): DocEntry {
            return DocEntry(league).apply(builder)
        }
    }

    var customDataSid: String? = null
    var killProcessor: StatProcessor
        get() = error("Not implemented")
        set(value) = killProcessors.add(value).let {}
    var deathProcessor: StatProcessor
        get() = error("Not implemented")
        set(value) = deathProcessors.add(value).let {}
    private val killProcessors: MutableSet<StatProcessor> = mutableSetOf()
    private val deathProcessors: MutableSet<StatProcessor> = mutableSetOf()
    var winProcessor: ResultStatProcessor? = null
    var looseProcessor: ResultStatProcessor? = null
    var resultCreator: (suspend AdvancedResult.() -> Unit)? = null
    var sorterData: SorterData? = null
    var setStatIfEmpty = false
    var numberMapper: (String) -> String = { it.ifEmpty { "0" } }
    var monsOrder: (List<DraftPokemon>) -> List<String> = { l -> l.map { it.name } }
    var cancelIf: (ReplayData, Int) -> Boolean = { _: ReplayData, _: Int -> false }
    var rowNumToIndex: (Int) -> Int = { it.minus(league.newSystemGap + 1).div(league.newSystemGap) }
    private val gamedays get() = league.gamedays
    fun newSystem(sorterData: SorterData, resultCreator: (suspend AdvancedResult.() -> Unit)) {
        val dataSheet = league.dataSheet
        val gap = league.newSystemGap
        killProcessor = BasicStatProcessor {
            Coord(
                sheet = dataSheet, gameday + 2, plindex.y(gap, monindex + 3)
            )
        }
        deathProcessor = BasicStatProcessor {
            Coord(
                sheet = dataSheet, gameday + 4 + gamedays, plindex.y(gap, monindex + 3)
            )
        }
        winProcessor = ResultStatProcessor {
            Coord(
                sheet = dataSheet, gameday + 2, plindex.y(gap, gap)
            )
        }
        looseProcessor = ResultStatProcessor {
            Coord(
                sheet = dataSheet, gameday + 4 + gamedays, plindex.y(gap, gap)
            )
        }
        this.resultCreator = resultCreator
        this.sorterData = sorterData
    }


    private fun generateForDay(size: Int, dayParam: Int): List<List<Long>> {
        val numDays = size - 1
        val day = dayParam - 1
        val table = league.table
        return buildList {
            add(listOf(table[day % numDays + 1], table[0]))
            for (idx in 1 until size / 2) {
                add(listOf(table[(day + idx) % numDays + 1], table[(day + numDays - idx) % numDays + 1]))
            }
        }

    }

    fun getMatchups(gameday: Int) =
        league.battleorder[gameday]?.map { mu -> mu.map { league.table[it] } } ?: generateForDay(
            league.table.size, gameday
        )

    fun analyse(
        replayData: ReplayData
    ) {
        val (game, uid1, uid2, kills, deaths, _, url, gamedayData) = replayData
        val (gameday, battleindex, u1IsSecond) = gamedayData
        if (cancelIf(replayData, gameday)) return

        val sid = league.sid
        val b = RequestBuilder(sid)
        val customB = customDataSid?.let(::RequestBuilder)
        val dataB = customB ?: b
        val uids = listOf(uid1, uid2)
        val picks = league.providePicksForGameday(gameday)
        for ((i, uid) in uids.withIndex()) {
            val index = league.table.indexOf(uid)
            var monIndex = -1
            val generalStatProcessorData = StatProcessorData(
                plindex = index,
                gameday = gameday,
                battleindex = battleindex,
                u1IsSecond = u1IsSecond,
                fightIndex = i
            )

            fun Set<StatProcessor>.checkTotal(map: Map<String, Int>) {
                val total = map.values.sum()
                forEach { p ->
                    if (p is CombinedStatProcessor) {
                        with(p) {
                            val k = generalStatProcessorData.process()
                            dataB.addSingle(
                                k.toString(), total
                            )
                        }
                    }
                }
            }
            if (!killProcessors.all { it is CombinedStatProcessor } || !deathProcessors.all { it is CombinedStatProcessor }) {
                for (pick in monsOrder(picks[uid]!!)) {
                    monIndex++
                    val deathStr = getNumber(deaths[i], pick)
                    if (deathStr.isEmpty() && !setStatIfEmpty) continue
                    val death = deathStr.toInt()
                    val killsOfMon = getNumber(kills[i], pick).toInt()
                    val statProcessorData = generalStatProcessorData.withMonIndex(monIndex)
                    fun Set<StatProcessor>.check(data: Int) {
                        forEach { p ->
                            if (p is BasicStatProcessor) {
                                with(p) {
                                    val k = statProcessorData.process()
                                    dataB.addSingle(
                                        k.toString(), data
                                    )
                                }
                            }
                        }
                    }
                    killProcessors.check(killsOfMon)
                    deathProcessors.check(death)
                }
            }
            killProcessors.checkTotal(kills[i])
            deathProcessors.checkTotal(deaths[i])

            (if (game[i].winner) winProcessor else looseProcessor)?.let { p ->
                with(p) {
                    val k = generalStatProcessorData.process()
                    dataB.addSingle(k.toString(), 1)
                }
            }
            if (game[i].winner) {
                league.results["$uid1:$uid2"] = uid
            }
        }

        run {
            val winningIndex = (if (game[0].winner) uid1 else uid2).indexedBy(league.table)
            val leagueName = league.leaguename
            val gamedayTips = league.tipgame?.tips?.get(gameday)
            if (gamedayTips?.evaluated?.contains(battleindex) == true) return@run
            gamedayTips?.userdata?.entries?.filter { it.value[battleindex] == winningIndex }?.map { it.key }?.forEach {
                TipGamesDB.addPointToUser(it, leagueName)
            }
            gamedayTips?.evaluated?.add(battleindex)
        }
        runBlocking {
            resultCreator?.let {
                val numbers = gamedayData.numbers()
                AdvancedResult(
                    b = b,
                    gdi = gameday - 1,
                    index = battleindex,
                    numberOne = numbers[0],
                    numberTwo = numbers[1],
                    swappedNumbers = u1IsSecond,
                    url = url,
                    replayData = replayData,
                    league = league
                ).it()
            }
            league.save()
        }
        customB?.execute()
        b.withRunnable(3000) { sort() }.execute()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun sort() {
        try {
            sorterData?.run {
                val sid = league.sid
                val b = RequestBuilder(sid)
                logger.info("Start sorting...")
                for (num in formulaRange.indices) {
                    val formulaRange = formulaRange[num]
                    val formula =
                        Google[sid, formulaRange.run { if (newMethod) "$sheet!$xStart$yStart:$xStart$yEnd" else toString() }, true]
                    val points = Google[sid, formulaRange.toString(), false].toMutableList()
                    val orig: List<List<Any>?> = ArrayList(points)
                    val table = league.table
                    val indexerToUse: (String) -> Int by lazy {
                        if (newMethod) { str: String ->
                            rowNumToIndex(
                                str.replace("$", "").substring(league.dataSheet.length + 4).substringBefore(":").toInt()
                            )
                        } else indexer!!
                    }
                    points.sortWith { o1: List<Any>, o2: List<Any> ->
                        val arr = cols.toList()
                        val first = if (directCompare) arr.subList(0, arr.indexOf(-1)) else arr
                        val c = compareColumns(
                            o1, o2, *first.toIntArray()
                        )
                        if (c != 0) return@sortWith c
                        if (!directCompare) return@sortWith 0
                        val u1 = table[indexerToUse(formula[orig.indexOf(o1)][0].toString())]
                        val u2 = table[indexerToUse(formula[orig.indexOf(o2)][0].toString())]
                        val o = league.results
                        o["$u1:$u2"]?.let { return@sortWith if (it == u1) 1 else -1 }
                        o["$u2:$u1"]?.let { return@sortWith if (it == u1) 1 else -1 }
                        val second: List<Int> = arr.subList(arr.indexOf(-1), arr.size)
                        if (second.size > 1) return@sortWith compareColumns(
                            o1, o2, *second.drop(1).toIntArray()
                        )
                        0
                    }
                    points.reverse()
                    val namap = mutableMapOf<Int, List<Any>>()
                    for ((i, objects) in orig.withIndex()) {
                        namap[points.indexOf(objects)] = formula[i]
                    }
                    val sendname: MutableList<List<Any>?> = ArrayList()
                    for (j in points.indices) {
                        sendname.add(namap[j])
                    }
                    b.addAll(formulaRange.firstHalf, sendname)
                }
                b.execute()
            }
        } catch (ex: Exception) {
            logger.error("I hate my life", ex)
        }
    }
}

data class StatProcessorData(
    val plindex: Int,
    val gameday: Int,
    val battleindex: Int,
    val u1IsSecond: Boolean,
    val fightIndex: Int
) {
    var monindex: Int = -1
        get() = if (field == -1) error("monindex not set (must be BasicStatProcessor to be usable)") else field

    val gameplanIndex get() = if (u1IsSecond) 1 - fightIndex else fightIndex
    val gdi by lazy { gameday - 1 }

    fun withMonIndex(monindex: Int) = copy().apply { this.monindex = monindex }
}

interface StatProcessor

fun interface BasicStatProcessor : StatProcessor {
    fun StatProcessorData.process(): Coord
}

fun interface CombinedStatProcessor : StatProcessor {
    fun StatProcessorData.process(): Coord
}

fun interface ResultStatProcessor {
    fun StatProcessorData.process(): Coord
}

@Suppress("unused")
data class AdvancedResult(
    val b: RequestBuilder,
    val gdi: Int,
    val index: Int,
    val numberOne: Int,
    val numberTwo: Int,
    val swappedNumbers: Boolean,
    val url: String,
    val replayData: ReplayData,
    val league: League
) {
    val tableIndexes by lazy {
        (0..1).map { league.table.indexOf(replayData.uids[it]) }
    }
    val monIndexes by lazy {
        (0..1).map { num -> replayData.mons[num].map { indexPick(league.picks[replayData.uids[num]].names(), it) } }
    }
    val deaths by lazy {
        (0..1).map { replayData.deaths[it].values.map { s -> s == 1 } }
    }
    val kills by lazy {
        (0..1).map {
            replayData.kills[it].values.toList()
        }
    }
    val winnerIndex by lazy { replayData.game.indexOfFirst { it.winner } }
    val higherNumber by lazy { if (numberOne > numberTwo) numberOne else numberTwo }
    fun Int.swap() = if (swappedNumbers) 1 - this else this
    val defaultGameplanString get() = """=HYPERLINK("$url"; "$numberOne:$numberTwo")"""
}


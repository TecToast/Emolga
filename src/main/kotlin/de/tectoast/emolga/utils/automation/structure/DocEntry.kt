package de.tectoast.emolga.utils.automation.structure

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.Command.Companion.compareColumns
import de.tectoast.emolga.commands.Command.Companion.getNumber
import de.tectoast.emolga.commands.Command.Companion.indexPick
import de.tectoast.emolga.database.exposed.TipGamesDB
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.records.StatLocation
import org.slf4j.LoggerFactory

class DocEntry private constructor(val league: League) {
    companion object {
        private val logger = LoggerFactory.getLogger(DocEntry::class.java)

        private val invalidProcessor: BasicStatProcessor = BasicStatProcessor { _, _, _ -> StatLocation.invalid() }

        fun create(league: League, builder: DocEntry.() -> Unit): DocEntry {
            return DocEntry(league).apply(builder)
        }
    }

    var killProcessor: StatProcessor = invalidProcessor
    var deathProcessor: StatProcessor = invalidProcessor
    private var useProcessor: BasicStatProcessor = invalidProcessor
    var winProcessor: ResultStatProcessor? = null
    var looseProcessor: ResultStatProcessor? = null
    var resultCreator: (AdvancedResult.() -> Unit)? = null
    var sorterData: SorterData? = null
    var setStatIfEmpty = false
    var numberMapper: (String) -> String = { it.ifEmpty { "0" } }
    var monsOrder: (List<DraftPokemon>) -> List<String> = { l -> l.map { it.name } }
    private var onlyKilllist: (() -> List<String>)? = null
    var randomGamedayMapper: (Int) -> Int = { it }
    var cancelIf: (ReplayData, Int) -> Boolean = { _: ReplayData, _: Int -> false }
    private val gamedays get() = league.gamedays
    fun newSystem(sorterData: SorterData, resultCreator: (AdvancedResult.() -> Unit)) {
        val dataSheet = league.dataSheet
        val gap = league.newSystemGap
        killProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation(
                sheet = dataSheet, gameday + 2, plindex.y(gap, monindex + 3)
            )
        }
        deathProcessor = BasicStatProcessor { plindex, monindex, gameday ->
            StatLocation(
                sheet = dataSheet, gameday + 4 + gamedays, plindex.y(gap, monindex + 3)
            )
        }
        winProcessor = ResultStatProcessor { plindex, gameday ->
            StatLocation(
                sheet = dataSheet, gameday + 2, plindex.y(gap, gap)
            )
        }
        looseProcessor = ResultStatProcessor { plindex, gameday ->
            StatLocation(
                sheet = dataSheet, gameday + 4 + gamedays, plindex.y(gap, gap)
            )
        }
        this.resultCreator = resultCreator
        this.sorterData = sorterData
    }


    private fun generateForDay(size: Int, dayParam: Int): List<List<Long>> {
        val numDays = size - 1
        val day = randomGamedayMapper(dayParam) - 1
        val table = league.table
        return buildList {
            add(listOf(table[day % numDays + 1], table[0]))
            for (idx in 1 until size / 2) {
                add(listOf(table[(day + idx) % numDays + 1], table[(day + numDays - idx) % numDays + 1]))
            }
        }

    }

    /**
     * generate the gameplan coords
     * @param u1 the first user
     * @param u1 the second user
     * @return a triple containing the gameday, the battle index and if p1 is the second user
     */
    private fun gameplanCoords(u1: Long, u2: Long): Triple<Int, Int, Boolean> {
        val size = league.table.size
        val numDays = size - 1
        val halfSize = size / 2
        val list = league.table.run { listOf(indexOf(u1), indexOf(u2)) }
        for (day in 0 until numDays) {
            val teamIdx = day % numDays + 1
            if (0 in list) {
                if (list[1 - list.indexOf(0)] == teamIdx) return Triple(randomGamedayMapper(day + 1), 0, list[0] == 0)
                continue
            }
            for (idx in 1 until halfSize) {
                val firstTeam = (day + idx) % numDays + 1
                val secondTeam = (day + numDays - idx) % numDays + 1
                if (firstTeam in list) {
                    if (list[1 - list.indexOf(firstTeam)] == secondTeam) return Triple(
                        randomGamedayMapper(day + 1), idx, list[0] == secondTeam
                    )
                    break
                }
            }
        }
        error("Didnt found matchup for $u1 & $u2 in ${Emolga.get.drafts.reverseGet(league)}")
    }

    fun getMatchups(gameday: Int) =
        league.battleorder[gameday]?.map { mu -> mu.map { league.table[it] } } ?: generateForDay(
            league.table.size, gameday
        )


    fun analyse(
        replayData: ReplayData
    ) {
        val (game, uid1, uid2, kills, deaths, _, url) = replayData
        var battleind = -1
        var u1IsSecond = false
        val i1 = league.table.indexOf(uid1)
        val i2 = league.table.indexOf(uid2)
        val gameday = if (league.battleorder.isNotEmpty()) league.battleorder.asIterable().reversed()
            .firstNotNullOfOrNull {
                if (it.value.any { l ->
                        l.containsAll(listOf(i1, i2)).also { b ->
                            if (b) u1IsSecond = l.indexOf(i1) == 1
                        }
                    }) it.key else null
            }
            ?: -1 else gameplanCoords(uid1, uid2).also {
            battleind = it.second
            u1IsSecond = it.third
        }.first
        val sid = league.sid
        val b = RequestBuilder(sid)
        onlyKilllist?.run {
            val mons = this()
            var monIndex = -1
            for (pick in mons) {
                monIndex++
                for (i in 0..1) {
                    val death = getNumber(
                        deaths[i], pick
                    )
                    if (death.isEmpty() && !setStatIfEmpty) continue
                    val k = (killProcessor as BasicStatProcessor).process(0, monIndex, gameday)
                    if (k.isValid) b.addSingle(
                        k.toString(), numberMapper(
                            getNumber(kills[i], pick)
                        )
                    )
                    val d = (deathProcessor as BasicStatProcessor).process(0, monIndex, gameday)
                    if (d.isValid) b.addSingle(
                        d.toString(), numberMapper(death)
                    )
                    val u = useProcessor.process(0, monIndex, gameday)
                    if (u.isValid) b.addSingle(
                        u.toString(), numberMapper("1")
                    )
                }
            }
            b.execute()
            return
        }
        val uids = listOf(uid1, uid2)
        val indices = listOf(i1, i2)
        val picksJson = league.picks
        for ((i, uid) in uids.withIndex()) {
            val index = league.table.indexOf(uid)
            var monIndex = -1
            var totalKills = 0
            var totalDeaths = 0
            for (pick in monsOrder(picksJson[uid]!!)) {
                monIndex++
                val death = getNumber(deaths[i], pick)
                if (death.isEmpty() && !setStatIfEmpty) continue
                if (killProcessor is BasicStatProcessor) {
                    val k = (killProcessor as BasicStatProcessor).process(index, monIndex, gameday)
                    if (k.isValid) b.addSingle(
                        k.toString(), numberMapper(
                            getNumber(
                                kills[i], pick
                            )
                        )
                    )
                } else if (killProcessor is CombinedStatProcessor) {
                    totalKills += getNumber(kills[i], pick).toInt()
                }
                if (deathProcessor is BasicStatProcessor) {
                    val d = (deathProcessor as BasicStatProcessor).process(index, monIndex, gameday)
                    if (d.isValid) b.addSingle(d.toString(), numberMapper(death))
                } else if (deathProcessor is CombinedStatProcessor) {
                    totalDeaths += death.toInt()
                }
                val u = useProcessor.process(index, monIndex, gameday)
                if (u.isValid) b.addSingle(u.toString(), numberMapper("1"))
            }
            (if (game[i].winner) winProcessor else looseProcessor)?.process(index, gameday)
                ?.run { b.addSingle(this.toString(), 1) }
            if (killProcessor is CombinedStatProcessor) {
                b.addSingle((killProcessor as CombinedStatProcessor).process(index, gameday).toString(), totalKills)
            }
            if (deathProcessor is CombinedStatProcessor) {
                b.addSingle((deathProcessor as CombinedStatProcessor).process(index, gameday).toString(), totalDeaths)
            }
            if (game[i].winner) {
                league.results["$uid1:$uid2"] = uid
            }
        }
        val (battleindex, numbers) = league.battleorder[gameday]?.let {
            val battleorder = league.battleorder[gameday]!!
            val battleusers = battleorder.firstOrNull { it.contains(i1) }.orEmpty()
            (battleorder.indices.firstOrNull { battleorder[it].contains(i1) } ?: -1) to (0..1).asSequence()
                .sortedBy { battleusers.indexOf(indices[it]) }.map { game[it].pokemon.count { m -> !m.isDead } }
                .toList()
        } ?: run {
            battleind to (0..1).map { game[it].pokemon.count { m -> !m.isDead } }
                .let { if (u1IsSecond) it.reversed() else it }
        }
        run {
            val winningIndex = (if (game[0].winner) uid1 else uid2).indexedBy(league.table)
            val leagueName = league.name
            val gamedayTips = league.tipgame?.tips?.get(gameday)
            if (gamedayTips?.evaluated?.contains(battleindex) == true) return@run
            gamedayTips?.userdata?.entries?.filter { it.value[battleindex] == winningIndex }?.map { it.key }?.forEach {
                TipGamesDB.addPointToUser(it, leagueName)
            }
            gamedayTips?.evaluated?.add(battleindex)
        }
        resultCreator?.let {
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
        saveEmolgaJSON()
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
                    points.sortWith { o1: List<Any>, o2: List<Any> ->
                        val arr = cols.toList()
                        val first = if (directCompare) arr.subList(0, arr.indexOf(-1)) else arr
                        val c = compareColumns(
                            o1, o2, *first.toIntArray()
                        )
                        if (c != 0) return@sortWith c
                        if (!directCompare) return@sortWith 0
                        val indexerToUse: (String) -> Int = if (newMethod) { str: String ->
                            str.substring(league.dataSheet.length + 4).substringBefore(":").toInt()
                                .minus(league.newSystemGap + 1)
                                .div(league.newSystemGap)
                        } else indexer!!
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


interface StatProcessor

fun interface BasicStatProcessor : StatProcessor {
    fun process(plindex: Int, monindex: Int, gameday: Int): StatLocation
}

fun interface CombinedStatProcessor : StatProcessor {
    fun process(plindex: Int, gameday: Int): StatLocation
}

fun interface ResultStatProcessor {
    fun process(plindex: Int, gameday: Int): StatLocation
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


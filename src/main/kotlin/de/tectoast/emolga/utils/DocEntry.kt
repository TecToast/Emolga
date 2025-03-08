package de.tectoast.emolga.utils

import de.tectoast.emolga.database.Database
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.league.GamedayData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.VideoProvideStrategy
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.MatchResult
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.SorterData
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.bson.Document
import org.litote.kmongo.eq
import org.litote.kmongo.regex
import org.slf4j.LoggerFactory


class DocEntry private constructor(val league: League) {
    companion object {
        private val logger = LoggerFactory.getLogger(DocEntry::class.java)

        fun create(league: League, builder: DocEntry.() -> Unit): DocEntry {
            return DocEntry(league).apply(builder)
        }
    }

    var customDataSid: String? = null
    var spoilerDocSid: String? = null
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
    var sorterData: SorterData?
        get() = error("Not implemented")
        set(value) {
            if (value != null)
                sorterDatas["default"] = value
        }
    private val sorterDatas = mutableMapOf<String, SorterData>()
    var setStatIfEmpty = false
    var monsOrder: (List<DraftPokemon>) -> List<String> = { l -> l.map { it.name } }
    var cancelIf: (ReplayData, Int) -> Boolean = { _: ReplayData, _: Int -> false }
    var rowNumToIndex: (Int) -> Int = { it.minus(league.newSystemGap + 1).div(league.newSystemGap) }
    private val gamedays get() = league.gamedays

    fun newSystem(sorterData: SorterData?, resultCreator: (suspend AdvancedResult.() -> Unit)) {
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


    suspend fun analyse(replayData: List<ReplayData>, withSort: Boolean = true) {
        val store = league.config.replayDataStore
        if (store != null) {
            replayData.forEach(league::storeMatch)
            league.save("DocEntry#Analyse")
            spoilerDocSid?.let { analyseWithoutCheck(replayData, withSort, overrideSid = it) }
            val gameday = replayData.first().gamedayData.gameday
            val currentDay = RepeatTask.getTask(league.leaguename, RepeatTaskType.RegisterInDoc)?.findGamedayOfWeek()
                ?: Int.MAX_VALUE
            if (currentDay <= gameday)
                return
        }
        analyseWithoutCheck(replayData, withSort)
    }

    suspend fun analyseWithoutCheck(
        replayDatas: List<ReplayData>,
        withSort: Boolean = true,
        realExecute: Boolean = true,
        overrideSid: String? = null
    ) {
        val matchResultJobs = mutableSetOf<Job>()
        val sid = overrideSid ?: league.sid
        val b = RequestBuilder(sid)
        val customB = customDataSid?.let(::RequestBuilder)
        val dataB = customB ?: b
        val winsSoFar = mutableMapOf<Int, Int>()
        val loosesSoFar = mutableMapOf<Int, Int>()
        for ((index, replayData) in replayDatas.withIndex()) {
            league.onReplayAnalyse(replayData)
            val (game, uindices, kd, _, url, gamedayData, otherForms, _) = replayData
            val (gameday, battleindex, u1IsSecond, _) = gamedayData
            if (cancelIf(replayData, gameday)) return
            if (gameday == -1) return
            val sorted = uindices.sorted()
            val lookUpIndex = if (uindices[0] == sorted[0]) 0 else 1
            var totalKills = 0
            var totalDeaths = 0
            kd[lookUpIndex].values.forEach {
                totalKills += it.first
                totalDeaths += it.second
            }
            matchResultJobs += Database.dbScope.launch {
                ignoreDuplicatesMongo {
                    db.matchresults.insertOne(
                        MatchResult(
                            data = listOf(totalKills, totalDeaths),
                            indices = sorted,
                            leaguename = league.leaguename,
                            gameday = gameday,
                            matchNum = index
                        )
                    )
                }
            }

            val picks = league.providePicksForGameday(gameday)
            for ((i, idx) in uindices.withIndex()) {
                val generalStatProcessorData = StatProcessorData(
                    plindex = idx,
                    gameday = gameday,
                    battleindex = battleindex,
                    u1IsSecond = u1IsSecond,
                    fightIndex = i,
                    matchNum = index,
                    winsSoFar[idx] ?: 0,
                    loosesSoFar[idx] ?: 0
                )

                fun Set<StatProcessor>.checkTotal(total: Int) {
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
                if (idx in picks && !killProcessors.all { it is CombinedStatProcessor } || !deathProcessors.all { it is CombinedStatProcessor }) {
                    val monsInOrder = monsOrder(picks[idx]!!)
                    for ((mon, data) in kd[i]) {
                        val monIndex = monsInOrder.indexOfFirst {
                            it == mon || otherForms[mon]?.contains(it) == true
                        }
                        if (monIndex == -1) {
                            logger.warn("Mon $mon not found in picks of $idx")
                            SendFeatures.sendToMe("Mon $mon not found in picks of $idx\n$url\n${league.leaguename}")
                            continue
                        }
                        val (kills, deaths) = data
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
                        killProcessors.check(kills)
                        deathProcessors.check(deaths)
                    }
                }
                val currentPlayerIsFirst = (lookUpIndex == 0) xor (i == 1)
                killProcessors.checkTotal(if (currentPlayerIsFirst) totalKills else totalDeaths)
                deathProcessors.checkTotal(if (currentPlayerIsFirst) totalDeaths else totalKills)

                (if (game[i].winner) {
                    winsSoFar.add(idx, 1)
                    winProcessor
                } else {
                    loosesSoFar.add(idx, 1)
                    looseProcessor
                })?.let { p ->
                    with(p) {
                        val k = generalStatProcessorData.process()
                        dataB.addSingle(k.toString(), 1)
                    }
                }
            }
        }
        val firstData = replayDatas.first()
        val gamedayData = firstData.gamedayData
        val (gameday, battleindex, u1IsSecond, _) = gamedayData
        val bo3 = replayDatas.size > 1
        val winningIndex: Int
        if (bo3) {
            val sdNamesOfFirstGame = firstData.game.map { it.sdPlayer?.nickname }
            val uindicesOfFirstGame = firstData.uindices
            val groupBy = replayDatas.groupBy { it.game.first { g -> g.winner }.sdPlayer?.nickname }
            winningIndex =
                uindicesOfFirstGame[groupBy
                    .maxByOrNull { it.value.size }?.key.indexedBy(sdNamesOfFirstGame)]
            league.config.tipgame?.let { tg ->
                TipGameUserData.updateCorrectBattles(league.leaguename, gameday, battleindex, winningIndex)
            }
            val numbers =
                sdNamesOfFirstGame.map { groupBy[it]?.size ?: 0 }.let { if (u1IsSecond) it.reversed() else it }
            resultCreator?.let {
                AdvancedResult(
                    b = b,
                    gdi = gameday - 1,
                    index = battleindex,
                    numberOne = numbers[0],
                    numberTwo = numbers[1],
                    swappedNumbers = u1IsSecond,
                    url = "",
                    replayData = firstData,
                    league = league
                ).it()
            }
        } else {
            val game = firstData.game
            val uindices = firstData.uindices
            winningIndex = (if (game[0].winner) uindices[0] else uindices[1])
            league.config.tipgame?.let { tg ->
                TipGameUserData.updateCorrectBattles(league.leaguename, gameday, battleindex, winningIndex)
            }
            val numbers = firstData.gamedayData.numbers
            resultCreator?.let {
                AdvancedResult(
                    b = b,
                    gdi = gameday - 1,
                    index = battleindex,
                    numberOne = numbers[0],
                    numberTwo = numbers[1],
                    swappedNumbers = u1IsSecond,
                    url = firstData.url,
                    replayData = firstData,
                    league = league
                ).it()
            }
        }
        customB?.execute(realExecute)
        b.withRunnable(3000) {
            if (withSort) {
                matchResultJobs.forEach { it.join() }
                sort()
            }
        }.execute(realExecute)
    }

    private fun compareColumns(o1: List<Any>, o2: List<Any>, vararg columns: Int): Int {
        for (column in columns) {
            val i1 = o1.getOrNull(column).parseInt()
            val i2 = o2.getOrNull(column).parseInt()
            if (i1 != i2) {
                return i1.compareTo(i2)
            }
        }
        return 0
    }

    fun Any?.parseInt() = (this as? Int) ?: this?.toString()?.toIntOrNull() ?: 0

    suspend fun sort(realExecute: Boolean = true) {
        try {
            (sorterDatas[league.leaguename] ?: sorterDatas["default"])?.run {
                val sid = league.sid
                val b = RequestBuilder(sid)
                logger.info("Start sorting...")
                for (num in formulaRangeParsed.indices) {
                    val formulaRange = formulaRangeParsed[num]
                    val formula =
                        Google.get(
                            sid,
                            formulaRange.run { if (newMethod) "$sheet!$xStart$yStart:$xStart$yEnd" else toString() },
                            true
                        )
                    val points = Google.get(sid, formulaRange.toString(), false)
                    val orig: List<List<Any>?> = ArrayList(points)
                    league.table
                    val indexerToUse by lazy {
                        if (newMethod) {
                            val new: suspend (String) -> Int = { str: String ->
                                rowNumToIndex(
                                    str.replace("$", "").substring(league.dataSheet.length + 4).substringBefore(":")
                                        .toInt()
                                )
                            }
                            new
                        } else indexer!!
                    }
                    val leagueCheckToUse =
                        if (includeAllLevels)
                            MatchResult::leaguename regex "^${
                                league.leaguename.dropLast(1)
                            }" else MatchResult::leaguename eq league.leaguename
                    val finalOrder = if (-1 !in cols) {
                        points.sortedWith(Comparator<List<Any>> { o1, o2 ->
                            compareColumns(o1, o2, *cols.toIntArray())
                        }.reversed())
                    } else {
                        val colsUntilDirectCompare = cols.takeWhile { it != -1 }
                        val colsAfterDirectCompare = cols.dropWhile { it != -1 }.drop(1)
                        val directCompare = points.groupBy {
                            colsUntilDirectCompare.map { col -> it.getOrNull(col).parseInt() }
                        }
                        val preSorted =
                            directCompare.entries.sortedWith(Comparator<Map.Entry<List<Int>, List<List<Any>>>> { o1, o2 ->
                                for (index in o1.key.indices) {
                                    val compare = o1.key[index].compareTo(o2.key[index])
                                    if (compare != 0) return@Comparator compare
                                }
                                0
                            }.reversed())
                        preSorted.flatMap { pre ->
                            val toCompare = pre.value
                            if (toCompare.size == 1) toCompare else {
                                val useridxs =
                                    toCompare.map { u -> indexerToUse(formula[orig.indexOf(u)][0].toString()) }
                                val allRelevantMatches = db.matchresults.find(
                                    leagueCheckToUse, Document(
                                        "\$expr", Document(
                                            "\$setIsSubset", listOf(
                                                "\$indices", useridxs
                                            )
                                        )
                                    )
                                ).toList()
                                val data = mutableMapOf<Int, DirectCompareData>()
                                useridxs.forEachIndexed { index, l ->
                                    data[l] = DirectCompareData(0, 0, 0, index)
                                }
                                allRelevantMatches.forEach {
                                    val uids = it.indices
                                    val winnerIndex = it.winnerIndex
                                    val winnerData = data[uids[winnerIndex]]!!
                                    val looserData = data[uids[1 - winnerIndex]]!!
                                    val killsForWinner = it.data[winnerIndex]
                                    val deathsForWinner = it.data[1 - winnerIndex]
                                    winnerData.points++
                                    winnerData.kills += killsForWinner
                                    winnerData.deaths += deathsForWinner
                                    looserData.kills += deathsForWinner
                                    looserData.deaths += killsForWinner
                                }
                                data.entries.sortedWith(Comparator<MutableMap.MutableEntry<Int, DirectCompareData>> { o1, o2 ->
                                    val compare = o1.value.points.compareTo(o2.value.points)
                                    if (compare != 0) return@Comparator compare
                                    for (directCompareOption in this.directCompare) {
                                        val compare1 = directCompareOption.getFromData(o1.value)
                                            .compareTo(directCompareOption.getFromData(o2.value))
                                        if (compare1 != 0) return@Comparator compare1
                                    }
                                    compareColumns(
                                        toCompare[o1.value.index],
                                        toCompare[o2.value.index],
                                        *colsAfterDirectCompare.toIntArray()
                                    )
                                }.reversed()).map { toCompare[it.value.index] }
                            }
                        }
                    }
                    val namap = mutableMapOf<Int, List<Any>>()
                    for ((i, objects) in orig.withIndex()) {
                        namap[finalOrder.indexOf(objects)] = formula[i]
                    }
                    val sendname: MutableList<List<Any>?> = ArrayList()
                    for (j in finalOrder.indices) {
                        sendname.add(namap[j])
                    }
                    b.addAll(formulaRange.firstHalf, sendname)
                }
                b.execute(realExecute)
            }
        } catch (ex: Exception) {
            logger.error("I hate my life", ex)
        }
    }
}

data class DirectCompareData(var points: Int, var kills: Int, var deaths: Int, val index: Int)

data class StatProcessorData(
    val plindex: Int,
    val gameday: Int,
    val battleindex: Int,
    val u1IsSecond: Boolean,
    val fightIndex: Int,
    val matchNum: Int = 0,
    val winCountSoFar: Int = 0,
    val looseCountSoFar: Int = 0
) {
    var monindex: Int = -1
        get() = if (field == -1) error("monindex not set (must be BasicStatProcessor to be usable)") else field

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

@Serializable
data class ReplayData(
    val game: List<DraftPlayer>,
    val uindices: List<Int>,
    val kd: List<Map<String, Pair<Int, Int>>>,
    val mons: List<List<String>>,
    val url: String,
    val gamedayData: GamedayData,
    val otherForms: Map<String, List<String>> = emptyMap(),
    val ytVideoSaveData: YTVideoSaveData = YTVideoSaveData()
) {
    suspend fun checkIfBothVideosArePresent(league: League): Boolean {
        val ytSave = ytVideoSaveData
        val shouldExecute = ytSave.vids.size == uindices.size
        val sendChannel = league.config.youtube?.sendChannel ?: return false
        if (shouldExecute) {
            league.executeYoutubeSend(
                sendChannel,
                gamedayData.gameday,
                gamedayData.battleindex,
                VideoProvideStrategy.Subscribe(ytSave)
            )
        }
        return shouldExecute
    }
}

@Serializable
data class YTVideoSaveData(
    var enabled: Boolean = false,
    val vids: MutableMap<Int, String> = mutableMapOf()
)

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
        replayData.uindices
    }
    val deaths by lazy {
        (0..1).map { replayData.kd[it].values.map { s -> s.second == 1 } }
    }
    val kills by lazy {
        (0..1).map {
            replayData.kd[it].values.map { p -> p.first }
        }
    }
    val winnerIndex by lazy { replayData.game.indexOfFirst { it.winner } }
    val higherNumber by lazy { if (numberOne > numberTwo) numberOne else numberTwo }
    fun Int.swap() = if (swappedNumbers) 1 - this else this
    val defaultGameplanString get() = """=HYPERLINK("$url"; "$numberOne:$numberTwo")"""
    val defaultGameplanStringWithoutUrl get() = "$numberOne:$numberTwo"
    val defaultSplitGameplanString get() = listOf("$numberOne", """=HYPERLINK("$url"; ":")""", "$numberTwo")
    val defaultSplitGameplanStringWithoutUrl get() = listOf("$numberOne", ":", "$numberTwo")
}


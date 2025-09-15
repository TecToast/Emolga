@file:OptIn(ExperimentalTime::class)

package de.tectoast.emolga.utils

import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.GamedayData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.VideoProvideStrategy
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.LeagueEvent
import de.tectoast.emolga.utils.json.LeagueEvent.MatchResult
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.TableSorter
import de.tectoast.emolga.utils.repeat.RepeatTask
import de.tectoast.emolga.utils.repeat.RepeatTaskType
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


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
    var monNameProcessor: StatProcessor
        get() = error("Not implemented")
        set(value) = monNameProcessors.add(value).let {}
    private val killProcessors: MutableSet<StatProcessor> = mutableSetOf()
    private val deathProcessors: MutableSet<StatProcessor> = mutableSetOf()
    private val monNameProcessors: MutableSet<StatProcessor> = mutableSetOf()
    var winProcessor: ResultStatProcessor? = null
    var looseProcessor: ResultStatProcessor? = null
    var resultCreator: (suspend AdvancedResult.() -> Unit)? = null
    var sorterData: TableSorter?
        get() = sorterDatas.firstOrNull()
        set(value) {
            sorterDatas += value!!
        }
    val sorterDatas = mutableSetOf<TableSorter>()
    var setStatIfEmpty = false
    var monsOrder: (List<DraftPokemon>) -> List<String> = { l -> l.map { it.name } }
    var cancelIf: (ReplayData, Int) -> Boolean = { _: ReplayData, _: Int -> false }
    var rowNumToIndex: (Int) -> Int = { it.minus(league.newSystemGap + 1).div(league.newSystemGap) }
    private val gamedays get() = league.gamedays

    fun newSystem(
        sorterData: TableSorter?,
        memberMod: Int? = null,
        dataSheetProvider: ((memidx: Int) -> String)? = null,
        resultCreator: (suspend AdvancedResult.() -> Unit)
    ) {
        newSystem(listOfNotNull(sorterData), memberMod, dataSheetProvider, resultCreator)
    }

    fun newSystem(
        sorterDatas: Collection<TableSorter>,
        memberMod: Int? = null,
        dataSheetProvider: ((memidx: Int) -> String)? = null,
        resultCreator: (suspend AdvancedResult.() -> Unit)
    ) {
        fun dataSheet(memidx: Int) = dataSheetProvider?.invoke(memidx) ?: league.dataSheet
        val gap = league.newSystemGap
        fun Int.mod() = memberMod?.let { this % it } ?: this
        killProcessor = BasicStatProcessor {
            Coord(
                sheet = dataSheet(plindex), gameday + 2, plindex.mod().y(gap, monindex + 3)
            )
        }
        deathProcessor = BasicStatProcessor {
            Coord(
                sheet = dataSheet(plindex), gameday + 4 + gamedays, plindex.mod().y(gap, monindex + 3)
            )
        }
        winProcessor = ResultStatProcessor {
            Coord(
                sheet = dataSheet(plindex), gameday + 2, plindex.mod().y(gap, gap)
            )
        }
        looseProcessor = ResultStatProcessor {
            Coord(
                sheet = dataSheet(plindex), gameday + 4 + gamedays, plindex.mod().y(gap, gap)
            )
        }
        this.resultCreator = resultCreator
        this.sorterDatas += sorterDatas
    }


    suspend fun analyse(replayData: List<ReplayData>, withSort: Boolean = true) {
        val config = league.config
        val store = config.replayDataStore
        if (store != null) {
            replayData.forEach(league::storeMatch)
            league.save("DocEntry#Analyse")
            spoilerDocSid?.let { analyseWithoutCheck(replayData, withSort, overrideSid = it) }
            val gameday = replayData.first().gamedayData.gameday
            val currentDay = RepeatTask.getTask(league.leaguename, RepeatTaskType.RegisterInDoc)?.findGamedayOfWeek()
                ?: Int.MAX_VALUE
            if (currentDay <= gameday)
                return
        } else if (config.triggers.saveReplayData) {
            replayData.forEach(league::storeMatch)
            league.save("DocEntry#Analyse")
        }
        analyseWithoutCheck(replayData, withSort)
    }

    suspend fun analyseWithoutCheck(
        replayDatas: List<ReplayData>,
        withSort: Boolean = true,
        realExecute: Boolean = true,
        overrideSid: String? = null
    ) {
        if (replayDatas.isEmpty()) return
        run {
            val firstReplayData = replayDatas.first()
            for (rd in replayDatas) {
                if (firstReplayData.uindices.size != rd.uindices.size || !firstReplayData.uindices.containsAll(rd.uindices)) {
                    logger.warn("ReplayDatas do not have the same players! Aborting processing...")
                    return
                }
            }
            league.config.tipgame?.let { _ ->
                league.executeTipGameLockButtonsIndividual(
                    firstReplayData.gamedayData.gameday,
                    firstReplayData.gamedayData.battleindex
                )
            }
        }
        val sortedUindices = replayDatas.first().uindices.sorted()
        val totalMonStats: MutableMap<Int, MutableMap<Int, MonStats>> = mutableMapOf()

        val matchResultJobs = mutableSetOf<Job>()
        val sid = overrideSid ?: league.sid
        val b = RequestBuilder(sid)
        val customB = customDataSid?.let(::RequestBuilder)
        val dataB = customB ?: b
        val winsSoFar = mutableMapOf<Int, Int>()
        val loosesSoFar = mutableMapOf<Int, Int>()
        for ((index, replayData) in replayDatas.withIndex()) {
            val (game, uindices, kd, _, _, gamedayData, otherForms, _) = replayData
            val (gameday, battleindex, u1IsSecond, _) = gamedayData
            if (cancelIf(replayData, gameday)) return
            if (gameday == -1) return
            val lookUpIndex = if (uindices[0] == sortedUindices[0]) 0 else 1
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
                            indices = sortedUindices,
                            leaguename = league.leaguename,
                            gameday = gameday,
                            matchNum = index,
                            timestamp = Clock.System.now()
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
                if (idx in picks && (!killProcessors.all { it is CombinedStatProcessor } || !deathProcessors.all { it is CombinedStatProcessor } || monNameProcessors.isNotEmpty())) {
                    val monsInOrder = monsOrder(picks[idx]!!)
                    for ((iterationIndex, tuple) in kd[i].entries.withIndex()) {
                        val (mon, data) = tuple
                        val monIndex = monsInOrder.indexOfFirst {
                            it == mon || otherForms[mon]?.contains(it) == true
                        }
                        if (monIndex == -1) {
                            logger.warn("Mon $mon not found in picks of $idx")
                            continue
                        }
                        totalMonStats.getOrPut(idx) { mutableMapOf() }.getOrPut(monIndex) { MonStats() }.add(data)
                        val (kills, deaths) = data
                        val statProcessorData = generalStatProcessorData.withMonIndex(monIndex, iterationIndex)
                        fun Set<StatProcessor>.check(data: Any) {
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
                        if (monNameProcessors.isNotEmpty()) {
                            val tlName = NameConventionsDB.convertOfficialToTL(mon, league.guild)
                                ?: error("No TL name found for $mon in ${league.leaguename} (${league.guild})")
                            monNameProcessors.check(tlName)
                        }
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
        if (killProcessors.any { it is Bo3BasicStatProcessor } || deathProcessors.any { it is Bo3BasicStatProcessor } || monNameProcessors.any { it is Bo3BasicStatProcessor }) {
            for (idx in sortedUindices) {
                // TODO: rework this whole statprocessordata thing
                val statProcessorData = StatProcessorData(
                    plindex = idx,
                    gameday = gameday,
                    battleindex = battleindex,
                    u1IsSecond = u1IsSecond,
                    fightIndex = 0,
                    matchNum = 0,
                    winCountSoFar = winsSoFar[idx] ?: 0,
                    looseCountSoFar = loosesSoFar[idx] ?: 0
                )
                totalMonStats[idx]?.forEach { (monIndex, stats) ->
                    val spd = statProcessorData.withMonIndex(monIndex, -1)
                    killProcessors.forEach { p ->
                        if (p is Bo3BasicStatProcessor) {
                            with(p) {
                                val k = spd.process()
                                dataB.addSingle(
                                    k.toString(), stats.kills
                                )
                            }
                        }
                    }
                    deathProcessors.forEach { p ->
                        if (p is Bo3BasicStatProcessor) {
                            with(p) {
                                val k = spd.process()
                                dataB.addSingle(
                                    k.toString(), stats.deaths
                                )
                            }
                        }
                    }
                }
            }
        }
        val winningIndex: Int
        if (bo3) {
            val uindicesOfFirstGame = firstData.uindices
            val groupBy = replayDatas.groupBy { it.game.indexOfFirst { g -> g.winner } }
            winningIndex = uindicesOfFirstGame[groupBy.maxBy { it.value.size }.key]
            league.config.tipgame?.let { _ ->
                TipGameUserData.updateCorrectBattles(league.leaguename, gameday, battleindex, winningIndex)
            }
            val numbers =
                (0..1).map { groupBy[it]?.size ?: 0 }.let { if (u1IsSecond) it.reversed() else it }
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
            league.config.tipgame?.let { _ ->
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
                matchResultJobs.joinAll()
                sort()
            }
        }.execute(realExecute)
    }

    suspend fun sort(realExecute: Boolean = true) {
        if (sorterDatas.isEmpty()) return
        val sid = league.sid
        val b = RequestBuilder(sid)
        logger.info("Start sorting...")
        for (tableSorter in sorterDatas) {
            val finalFormula = tableSorter.getSortedFormulas()
            b.addAll(tableSorter.formulaRangeParsed.firstHalf, finalFormula)
        }
        b.execute(realExecute)

    }
}

data class UserTableData(
    var points: Int = 0,
    var kills: Int = 0,
    var deaths: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    val index: Int
) {
    val diff get() = kills - deaths
    val wlRatio get() = if (losses == 0) Double.MAX_VALUE else wins.toDouble() / losses.toDouble()

    companion object {
        fun createFromEvents(idxs: List<Int>, events: List<LeagueEvent>): MutableMap<Int, UserTableData> {
            val data = mutableMapOf<Int, UserTableData>()
            idxs.forEachIndexed { index, l ->
                data[l] = UserTableData(index = index)
            }
            events.forEach {
                it.manipulate(data)
            }
            return data
        }
    }
}

private data class MonStats(var kills: Int = 0, var deaths: Int = 0) {
    fun add(stats: Pair<Int, Int>) {
        kills += stats.first
        deaths += stats.second
    }
}

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

    var monIterationIndex: Int = -1
        get() = if (field == -1) error("monIterationIndex not set (must be BasicStatProcessor to be usable)") else field

    val gdi by lazy { gameday - 1 }

    fun withMonIndex(monindex: Int, iterationIndex: Int) =
        copy().apply { this.monindex = monindex; this.monIterationIndex = iterationIndex }
}

interface StatProcessor

fun interface BasicStatProcessor : StatProcessor {
    fun StatProcessorData.process(): Coord
}

fun interface CombinedStatProcessor : StatProcessor {
    fun StatProcessorData.process(): Coord
}

fun interface Bo3BasicStatProcessor : StatProcessor {
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
    // TODO: is mons obsolete? because all mons are already in kd (rework the whole thing)
    val mons: List<List<String>>,
    val url: String,
    // TODO: For Bo3, all gamedayData are the same, so we could also just have one
    val gamedayData: GamedayData,
    val otherForms: Map<String, List<String>> = emptyMap(),
    val ytVideoSaveData: YTVideoSaveData = YTVideoSaveData()
) {
    suspend fun checkIfBothVideosArePresent(league: League) {
        val ytSave = ytVideoSaveData
        val shouldExecute = ytSave.vids.size == uindices.size
        val sendChannel = league.config.youtube?.sendChannel ?: return
        if (shouldExecute) {
            league.executeYoutubeSend(
                sendChannel,
                gamedayData.gameday,
                gamedayData.battleindex,
                VideoProvideStrategy.Subscribe(ytSave)
            )
        }
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
    val idxs by lazy {
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
    val lowerNumber by lazy { if (numberOne < numberTwo) numberOne else numberTwo }
    fun Int.swap() = if (swappedNumbers) 1 - this else this
    val defaultGameplanString get() = """=HYPERLINK("$url"; "$numberOne:$numberTwo")"""
    val defaultGameplanStringWithoutUrl get() = "$numberOne:$numberTwo"
    val defaultSplitGameplanString get() = listOf("$numberOne", """=HYPERLINK("$url"; ":")""", "$numberTwo")
    val defaultSplitGameplanStringWithoutUrl get() = listOf("$numberOne", ":", "$numberTwo")
}

